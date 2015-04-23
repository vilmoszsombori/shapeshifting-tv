package tv.shapeshifting.nsl;

import java.util.HashMap;

import tv.shapeshifting.nsl.exceptions.UnrecognizedMediatypeException;

public class MediaTypeMap {
	private static MediaTypeMap instance = null;
	private HashMap<Object, String> map = new HashMap<Object, String>();
	
	private MediaTypeMap() {
		map.put("1", "audio");
		map.put("audio", "audio");
		map.put("2", "video");
		map.put("video", "video");				
	}
		
	public static MediaTypeMap i() {
		if ( instance == null )
			instance = new MediaTypeMap();
		return instance;			
	}
	
	public String get(Object key) throws UnrecognizedMediatypeException {
		if ( key == null )
			new UnrecognizedMediatypeException("Empty media type." );
		
		key = key.toString().trim().toLowerCase();
		
		if ( map.containsKey(key) )
			return map.get(key);
		else
			throw new UnrecognizedMediatypeException("Media type [" + key + "] is not recognized." );
	}
}
