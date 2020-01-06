package edu.udo.cs.rvs.ssdp;

import java.io.*;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;

public class Worker implements Runnable {
    private final String UNICAST = "HTTP/1.1 200 OK";
    private final String MULTICAST = "NOTIFY * HTTP/1.1";
    private final String SERV_TYP_ST = "ST: ";
    private final String SERV_TYP_NT = "NT: ";
    private final String UUID_USN = "USN: uuid:";
    private final String NTS = "NTS: ";
    private final String NTS_ALIVE = "ssdp:alive";
    private final String NTS_BYE = "ssdp:byebye";
    private final String EMPTY_LINE = "";

    private DatagramPacket dp;

    /**
     *  Datagramme aus dem Listen-Thread verarbeiten und dem User-Thread mitteilen,
     *  welche Geraete gerade Dienste im Netzwerk anbieten.
     */
    @Override
    public void run() {
        // Worker-Thread bis zum Programmende endlos laufen
        while(true) {
            // 10ms schlafen, wenn keine Datagramme vorliegen
            if (Listen.dgll.isEmpty()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // Fehlerbehandlung
                    e.printStackTrace();
                }

            }else {
                //das aelteste Datagramm nehmen und aus der Liste entfernen
                synchronized (Listen.dgll) {
                    dp = ((LinkedList<DatagramPacket>) Listen.dgll).removeFirst();
                }
                try (// Ein BufferedReader vorbreiten, um den Inhalt des SSDP-Pakets zeilenweise einzulesen
                     InputStream inputStream = new ByteArrayInputStream(dp.getData());
                     InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                     BufferedReader reader = new BufferedReader(streamReader)){
                    // Wenn der BufferedReader ist vorbreitet, den Inhalt des SSDP-Pakets einlesen.
                    if(reader.ready()){
                        // Die erste Zeile
                        String line = reader.readLine();
                        // Der Typ des Pakets
                        int typ = this.getReplyTyp(line);
                        // Ein Zeile von der Liste des Users
                        UserListLine userListLine = new UserListLine();
                        // Wenn wir uns den Antwort-Typ interessieren und die Zeile nicht die letzte Zeile ist,
                        // weiter eine Zeile einlesen.
                        while (typ!=0 && !EMPTY_LINE.equals(line)) {
                            line = reader.readLine();
                            // Ob der Antwort-Typ Multicast und die Zeile anfangen mit "NTS: " ist
                            if (typ==2 && line.startsWith(NTS)) {
                                // Ob eine Abmeldung ist und ein richtiges UUID schon eingefangen
                                if(NTS_BYE.equals(line.substring(NTS.length()))
                                        && userListLine.getUuid() != null) {
                                    // Dienste aus die User-Liste herausloesen
                                    serverCheckOut(userListLine.getUuid());
                                    // die UUID des jetzten Userlistline-Objekts null lassen,
                                    // um diese Zeile nicht in die User-Liste einschreiben.
                                    userListLine.setUuid(null);
                                    break;
                                }
                                // Beim Uebertragungsfehler ignorieren.
                                else if(!NTS_ALIVE.equals(line.substring(NTS.length()))){
                                    userListLine.setUuid(null);
                                    break;
                                }
                            }
                            else{
                                // Userlistline-Objekts setzen
                                setListLine(line, userListLine);
                            }
                        }
                        // Ob UserListLine-Objekt ein richtiges UUID und Dienst-Typ besetzt
                        if(userListLine.getUuid()!=null && userListLine.getServiceType()!=null
                        && isNotRepeat(userListLine)){
                            synchronized (User.serverList) {
                                // Jetzte Zeile in der User-Liste aufnehmen
                                User.serverList.add(userListLine);
                            }
                        }
                    }
                } catch (IOException e) {
                    // Fehlerbehandlung
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *  Auszuwerten, welcher Typ das Paket ist .
     *  @param t Die erst Zeile des Pakets, die den Typ des Pakets angibt.
     *  @return typ: 1.Unicast-Antwort; 2.Multicast-Antwort; 0.sonst
     */
    private int getReplyTyp(String t){
        int typ = 0;
        if(UNICAST.equals(t)) {
            typ = 1;
        }
        else if(MULTICAST.equals(t)){
            typ = 2;
        }
        return typ;
    }

    /**
     *  Zu Ueberpruefen, ob die eingelesene Zeile nicht in "serverList" enthaltet ist.
     *  @param userListLine Eingelesene Zeile
     *  @return true: Die Zeile ist nicht in "serverList" enthaltet; false: in "serverList" enthaltet
     */
    private boolean isNotRepeat(UserListLine userListLine){
        Iterator<UserListLine> it = User.serverList.iterator();
        while(it.hasNext()) {
            UserListLine compareLine = it.next();
            if(compareLine.getUuid().equals(userListLine.getUuid())
                    && compareLine.getServiceType().equals(userListLine.getServiceType())){
                return false;
            }
        }
        return true;
    }

    /**
     *  Ein UserListLine-Objekt setzen.
     *  @param line Eingelesene Zeile
     *  @param userListLine Ein neu inistalisiertes UserListLine-Objekt.
     */
    private void setListLine(String line, UserListLine userListLine){
        if (line.startsWith(SERV_TYP_ST)) {
            userListLine.setServiceType(line.substring(SERV_TYP_ST.length()));
        }
        else if (line.startsWith(SERV_TYP_NT)) {
            userListLine.setServiceType(line.substring(SERV_TYP_NT.length()));
        }
        else if (line.startsWith(UUID_USN) && (line.length()>=UUID_USN.length()+36)) {
            //System.out.println("this line is "+line);
            String uuid = line.substring(UUID_USN.length(),UUID_USN.length()+36);
            if(isUUID(uuid)) {
                userListLine.setUuid(uuid);
            }
        }
    }

    /**
     *  Zu ueberpruefen, ob der Parameter ein richtiges UUID ist
     *  @param str ein String, das ein UUID kann
     *  @return true: UUID; false: kein UUID
     */
    public static boolean isUUID(String str) {
        String regex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
        return str.matches(regex);
    }

    /**
     *  Dienste aus die bekannte Dienst-Liste herausloesen.
     *  @param uuid Eingelesenes UUID
     */
    private void serverCheckOut(String uuid){
        synchronized (User.serverList){
            Iterator<UserListLine> it = User.serverList.iterator();
            while(it.hasNext()){
                if(it.next().getUuid().equals(uuid)){
                    it.remove();
                }
            }
        }
    }
}
