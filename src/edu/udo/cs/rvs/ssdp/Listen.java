package edu.udo.cs.rvs.ssdp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

public class Listen implements Runnable  {


    private MulticastSocket ms;
    public static final List<DatagramPacket> dgll = new LinkedList<>();

    /**
     *  Konstruktor des Listen-Thread.
     *  Ein MulticastSocket auf Port 1900 oeffnen, der Multicast-Gruppe „239.255.255.250“ beitreten.
     */
    public Listen(){
        try {
            // InetAddress Objekt erhalten
            InetAddress grup = InetAddress.getByName(SSDPPeer.GROUP_IP);
            // Ein Multicast auf Port 1900 oeffnen
            ms = new MulticastSocket(SSDPPeer.PORT);
            // Multicast-Gruppe beitreten
            ms.joinGroup(grup);
        } catch (IOException e) {
            // Fehlerbehandlung
            e.printStackTrace();
        }

    }

    /**
     *  Datagramme empfangen und in eine Liste aufgenommen werden
     */
    @Override
    public void run() {
        // Listen-Thread bis zum Programmende endlos laufen
        while(ms.isBound() && !ms.isClosed()) {
            try {
                DatagramPacket dp = this.createDp();
                // Datagramme empfangen
                ms.receive(dp);
                // Datagramme in eine Liste aufgenommen werden
                addPacket(dp);
            } catch (IOException e) {
                // Fehlerbehandlung
                e.printStackTrace();
            }
        }
    }

    /**
     *  Datagramme in eine Liste aufgenommen werden
     *  @param dp in die Liste aufgenommenes DatagramPacket
     */
    private void addPacket(DatagramPacket dp) {
        synchronized (dgll) {
            dgll.add(dp);
        }
    }

    /**
     *  Einen neuen Puffer Erzeugn
     */
    private DatagramPacket createDp() throws SocketException {
        return new DatagramPacket(new byte[ms.getReceiveBufferSize()], ms.getReceiveBufferSize());
    }

    public MulticastSocket getMs() {
        return ms;
    }
}