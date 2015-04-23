package tv.shapeshifting.nsl.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class QueryFileManager {
	private static Logger LOG = Logger.getLogger(QueryFileManager.class);
	private HashMap<String, String> map;
	private static QueryFileManager i = null;
	
	private QueryFileManager() {
		LOG.info("initializing...");
		map = new HashMap<String, String>();
	}
	
	public static QueryFileManager i() {
		if(i == null) {
			i = new QueryFileManager();
		}
		return i;
	}
	
	public String get(String url) throws IOException {
		if(!map.containsKey(url)) {
			String r = readFileAsString(url);
			map.put(url, r);
		} 	
		return map.get(url);
	}
	
	private static String readFileAsString(String url) throws java.io.IOException {
		StringBuffer s = new StringBuffer();
		URL u = new URL(url);
		URLConnection c = u.openConnection();
		BufferedReader in = new BufferedReader(
				new InputStreamReader(c.getInputStream()));
		String inputLine;
        while ((inputLine = in.readLine()) != null) 
        	s.append(inputLine + "\n");
        in.close();
	    return s.toString();
	}		
}
