package tv.shapeshifting.nsl.ontology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class SparqlFileRepository {
	private static Logger LOG = Logger.getLogger(SparqlFileRepository.class);
	private HashMap<String, String> map;
	private static SparqlFileRepository i = null;
	
	private SparqlFileRepository() {
		LOG.info("initialized");
		map = new HashMap<String, String>();
	}
	
	public static SparqlFileRepository i() {
		if(i == null) {
			i = new SparqlFileRepository();
		}
		return i;
	}
	
	public String get(String queryPath) throws IOException {
		if(!map.containsKey(queryPath)) {
			String r = load(queryPath);
			map.put(queryPath, r);
		} 	
		return map.get(queryPath);
	}
		
	private synchronized String load(String queryPath) throws java.io.IOException {
		StringBuffer query = new StringBuffer();
		BufferedReader reader;
		try {
			// try loading it as a URL
			URL url = new URL(queryPath);
			URLConnection connection = url.openConnection();
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		} catch (MalformedURLException e) {
			// try loading it from the classpath
			LOG.debug("Loading [" + queryPath + "]...");
			reader = new BufferedReader(new InputStreamReader(
				    this.getClass().getResourceAsStream(queryPath)));			
		}
		String inputLine;
        while ((inputLine = reader.readLine()) != null) 
        	query.append(inputLine + "\n");
        reader.close();
	    return query.toString();
	}
	
	public static void main(String[] agrs) throws IOException {
		LOG.debug(SparqlFileRepository.i().get("queries/narrativeRoot.query"));
	}
}

