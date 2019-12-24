package edu.udo.cs.rvs.ssdp;


/**
 * This class is first instantiated on program launch and IF (and only if) it
 * implements Runnable, a {@link Thread} is created and started.
 *
 */
public class SSDPPeer implements Runnable
{
	public SSDPPeer()
	{

	}

    @Override
    public void run() {
        Listen l = new Listen();
        Worker w = new Worker();
        User u = new User(l.getMs());

        Thread a = new Thread(l);
        Thread b = new Thread(w);
        Thread c = new Thread(u);

        a.start();
        b.start();
        c.start();
    }
}
