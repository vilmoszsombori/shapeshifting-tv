package tv.shapeshifting.nsl;

import org.apache.log4j.Logger;

public class Barriers {
	private static Logger LOG = Logger.getLogger(Barriers.class);	
	private final Object playlistBarrier = new Object();
	
	public Barriers() {
		super();
		LOG.debug("Barriers initialized.");
	}
	
	public void signal() {
		synchronized (playlistBarrier) {
			playlistBarrier.notifyAll();			
		}
	}
	
	public void await() throws InterruptedException {
		synchronized (playlistBarrier) {
			playlistBarrier.wait();
		}
	}	
}
