package tv.shapeshifting.nsl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.exceptions.TimecodeFormatException;
import tv.shapeshifting.nsl.util.Timecode;

public class Playlist {
	private static Logger LOG = Logger.getLogger(Playlist.class);	
	static final int MAXQUEUE = 5;
	private Vector<Map<String, Vector<Object>>> playlist = new Vector<Map<String, Vector<Object>>>();
	private Vector<Map<String, Vector<Object>>> history = new Vector<Map<String, Vector<Object>>>();
	private Map<String, Vector<Object>> fragment = new ConcurrentHashMap<String, Vector<Object>>();
	private Map<String, Timecode> duration = new ConcurrentHashMap<String, Timecode>();
		
	public Playlist() {
		LOG.debug("New playlist container created.");		
	}
	
	// called by the producer 
	private synchronized void putFragment(Map<String, Vector<Object>> fragment) throws InterruptedException {
        while ( playlist.size() == MAXQUEUE ) 
            wait(); 
        playlist.addElement(fragment); 
        notify(); 		
	}
	
	// called by consumer
	public synchronized Map<String, Vector<Object>> getFragment() throws InterruptedException {
		notify();
		while ( playlist.size() == 0 )
			wait();
		Map<String, Vector<Object>> fragment = playlist.firstElement();
		playlist.removeElement(fragment);
		history.add(fragment);
		return fragment;				
	}	
			
	// called by NSL interpreter threads 
	public synchronized long add(String key, Object value) throws InterruptedException, TimecodeFormatException {
		while ( fragment == null )
			wait(); // wait for the flush to complete
		if ( !fragment.containsKey(key) )
			fragment.put(key, new Vector<Object>());
		if ( !duration.containsKey(key) )
			duration.put(key, Timecode.valueOf(0));
		
		long ret = 0;
		
		fragment.get(key).add(value);
		if (value instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> map = (Map<Object, Object>) value;
			if ( map.containsKey("clipBegin") && map.containsKey("clipEnd") ) {
				map.put("begin",  duration.get(key).toSeconds());
				duration.put(key, Timecode.valueOf(
						duration.get(key).longValue() + 
						Timecode.parse(map.get("clipEnd")).longValue() - 
						Timecode.parse(map.get("clipBegin")).longValue()));
				ret = duration.get(key).longValue();
			} else if ( map.containsKey("dur") ) {
				//map.put("begin",  duration.get(key).toSeconds());
				duration.put(key, Timecode.valueOf(
						duration.get(key).longValue() + 
						Timecode.parse(map.get("dur")).longValue()));
				ret = duration.get(key).longValue();
			}
		}			
		notify();
		return ret;
	}
	
	public synchronized Timecode duration(String key) throws InterruptedException, TimecodeFormatException {
		if ( duration.containsKey(key) )
			return duration.get(key);
		return new Timecode(0);
	}
	
	public synchronized long duration() {
		long ret = 0;
		if ( ! duration.isEmpty() ) {
			for(Iterator<String> it = duration.keySet().iterator(); it.hasNext(); ) {
				String key = it.next();
				ret = Math.max(ret, duration.get(key).longValue());				
			}
		}
		return ret;
	}
	
	// called by the ...
	public synchronized Map<String, Vector<Object>> flushFragment() throws InterruptedException {
		notify();
		
		// update durations
		fragment.put("dur", new Vector<Object>());
		Iterator<String> i = duration.keySet().iterator();		
		while ( i.hasNext() ) {
			String key = i.next();
			Map<String, String> m = new HashMap<String, String>();
			try {
				m.put(key, duration(key).toSeconds());
				fragment.get("dur").add(m);
			} catch(TimecodeFormatException e) {
				LOG.warn("Duration not added. " + e.getMessage());
			}
		}
		/*
		 * TODO get rid of the audio hack (implemented for Pablo's tests)
		 * 
		if ( fragment.containsKey("audio") && fragment.containsKey("video") ) {
			Vector<Object> audios = fragment.get("audio");
			Vector<Object> videos = fragment.get("video");
			if ( audios.size() > 0 && videos.size() > 0 ) {
				if ( audios.firstElement() instanceof Map<?, ?> && videos.firstElement() instanceof Map<?, ?> ) {
					Map<?, ?> firstVideo = (Map<?, ?>) videos.firstElement();					
					Map<?, ?> firstAudio = (Map<?, ?>) audios.firstElement();
					if ( firstVideo.containsKey("id") ) {
						((Map<Object, Object>)firstAudio).put("begin", firstVideo.get("id") + ".begin");
					}
				}
			}
		}
		 */
		
		/* TODO should empty fragments be allowed to be submitted?
		 * if not, there is a solution here:
		 * while ( fragment.isEmpty() )
		 * 	wait(); // wait for playlist elements to be added
		 */
		putFragment(fragment);
		Map<String, Vector<Object>> temp = fragment;
		fragment = new ConcurrentHashMap<String, Vector<Object>>();
		duration.clear();
		return temp;				
	}
	
	public synchronized Vector<Map<String, Vector<Object>>> getHistory() {
		notify();
		return this.history;
	}
}
