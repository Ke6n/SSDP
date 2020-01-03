package edu.udo.cs.rvs.ssdp;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class User implements Runnable{
    private final String CMD_EXIT = "EXIT";
    private final String CMD_CLEAR = "CLEAR";
    private final String CMD_LIST = "LIST";
    private final String CMD_SCAN = "SCAN";

    public static final List<UserListLine> serverList = new LinkedList<>();
    private MulticastSocket ms;

    // Ein MulticastSocket aus Listen-Thead sollen, um den SCAN-Befehl durchzufueren.
    public User(MulticastSocket ms) {
        this.ms = ms;
    }

    /**
     *  Die Nutzereingaben lesen, verarbeiten und entsprechende Aktionen durchführen.
     */
    @Override
    public void run() {

        while (true) {
            // Ein BufferedReader fuer zeilenweise Einlesen der Usereingabe
            InputStreamReader streamReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            try {
                // Wenn gerade kein Befehl vorliegt, warten der Thread 10ms,
                // um den Prozessor nicht unnötig zu beanspruchen.
                if (!reader.ready()) {
                    Thread.sleep(10);
                } else {
                    String command = reader.readLine();
                    // Ob die Eingabe ein Befehl ist
                    if(isCommand(command)) {
                        // EXIT-Befehl: Das Programm sich beenden
                        if(CMD_EXIT.equals(command)){
                            System.exit(0);
                        }
                        // CLEAR-Befehl: Alle Geräte vergessen werden
                        else if(CMD_CLEAR.equals(command)){
                            serverList.clear();
                        }
                        // LIST-Befehl: Alle bekannten Geräte gelistet werden
                        else if(CMD_LIST.equals(command)){
                            list();
                        }
                        // SCAN-Befehl: Ueber das MulticastSocket des Listen-Threads eine Suchanfrage versendet werden
                        else {
                            scan();
                        }
                    }
                    else {
                        // Behandlung der illegalen Eingabe des Users
                        System.out.println(String.format("%s ist nicht erwartet!", command));
                    }
                }
            } catch (InterruptedException | IOException e) {
                // Fehlerbehandlung
                e.printStackTrace();
            }
        }
    }

    /**
     *  Auszuwerten, ob die Eingabe ein Befehl ist.
     *  @param cmd Usereingabe
     *  @return true: Befehl; falsch: Illegale Eingabe
     */
    private boolean isCommand(String cmd){
        return CMD_EXIT.equals(cmd) || CMD_CLEAR.equals(cmd)
                || CMD_LIST.equals(cmd) || CMD_SCAN.equals(cmd);
    }

    /**
     *  Bekannte Dienstliste ausgeben.
     */
    private void list(){
        UserListLine uLine;
        synchronized (serverList) {
            Iterator<UserListLine> it = serverList.iterator();
            while (it.hasNext()) {
                uLine = it.next();
                System.out.println(uLine.getUserListStyle());
            }
        }
        System.out.println();
    }

    /**
     *  Behandlung des SCAN-Befehls: Ueber das MulticastSocket des Listen-Threads eine Suchanfrage versendet werden
     */
    private void scan(){
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(bos,false, StandardCharsets.UTF_8)) {
            if (ms != null) {
                setPacketData(ps);
                ps.flush();
                ms.send(new DatagramPacket(bos.toByteArray(), bos.size(),
                        InetAddress.getByName(SSDPPeer.GROUP_IP), SSDPPeer.PORT));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Inhalt eines DatagramPackets fuer SCAN-Befehl setzen
     */
    private void setPacketData(PrintStream ps){
        ps.println("M-SEARCH * HTTP/1.1");
        UUID id = UUID.randomUUID();
        //System.out.println("my uuid is: "+id);
        ps.println(String.format("S: uuid:%s", id));
        ps.println("HOST: 239.255.255.250:1900");
        ps.println("MAN: \"ssdp:discover\"");
        ps.println("ST: ssdp:all");
        ps.println();
    }
}
