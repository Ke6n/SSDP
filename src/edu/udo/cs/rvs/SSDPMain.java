package edu.udo.cs.rvs;

import edu.udo.cs.rvs.ssdp.SSDPPeer;

/**
 * The Main-Class containing the function responsible for starting this program
 *
 * DO NOT MODIFY!
 * 
 * @author RvS-Tutorenteam
 */
public class SSDPMain
{
	/**
	 * The Main-Function responsible for starting this program
	 *
	 * @param args
	 *            ignored, but required
	 */
	public static void main(String[] args)
	{
		SSDPPeer peer = new SSDPPeer();
		// Check if peer can be started as a Thread
		if (peer instanceof Runnable)
		{
			// Construct the Thread and start it (also set some properties)
			Runnable peerRunnable = (Runnable) peer;
			Thread peerThread = new Thread(peerRunnable);
			peerThread.setName("Peer Thread");
			peerThread.setDaemon(false);
			peerThread.start();
		}
	}
}
