package edu.udo.cs.rvs.ssdp;


/**
 * This class is first instantiated on program launch and IF (and only if) it
 * implements Runnable, a {@link Thread} is created and started.
 *
 */
public class SSDPPeer implements Runnable
{
    public static final int PORT = 1900;
    public static final String GROUP_IP = "239.255.255.250";

    /**
     *  Threads initialisiern.
     */
    @Override
    public void run() {
        Listen listen = new Listen();
        Worker worker = new Worker();
        User user = new User(listen.getMs());

        Thread listenThread = new Thread(listen);
        Thread workerThread = new Thread(worker);
        Thread userThread = new Thread(user);

        listenThread.start();
        workerThread.start();
        userThread.start();
    }
}
