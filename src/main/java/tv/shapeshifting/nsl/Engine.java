package tv.shapeshifting.nsl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.exceptions.UnexpectedNarrativeObjectException;
import tv.shapeshifting.nsl.ontology.Wrapper;

public class Engine {
	private static Logger LOG = Logger.getLogger(Engine.class);
	private static HashMap<String, NslInterpreter> interpreters = new HashMap<String, NslInterpreter>();

	public static String version() throws FileNotFoundException, IOException {
		String version = removeDollar(Settings.i().get("VERSION"));// + Settings.i().get("REVISION");
		return version;		
	}
	
	public static String revision() throws FileNotFoundException, IOException {
		String revision = removeDollar(Settings.i().get("REV"));
		return revision;
	}
	
	public static String date() throws FileNotFoundException, IOException {
		String date = removeDollar(Settings.i().get("DATE"));
		return date;
	}
	
	public static String author() throws FileNotFoundException, IOException {
		String author = removeDollar(Settings.i().get("AUTHOR"));
		return author;
	}
	
	private static String removeDollar(String s) {
		if ( s.startsWith("$") )
			s = s.substring(1);
		if ( s.endsWith("$") )
			s = s.substring(0, s.length() - 1);
		return s;
	}
		
	public static NslInterpreter i(String sessionId) throws MalformedURLException, IOException {
		if(interpreters.containsKey(sessionId)) {
			return interpreters.get(sessionId);
		} else {
			// TODO implement support for non-hardcoded narratives
			String[] narratives = {"http://localhost/ShapeShift/rs/owl/ta2myvideos.production.2.owl"};
			OntologyInterface ow = new Wrapper(narratives, null);			
			NslInterpreter i = new NslInterpreter(ow);
			return i;
		}
	}

	public static NslInterpreter remove(String sessionId) {
		return interpreters.remove(sessionId);
	}
	
	public static void shutdown() {
		interpreters.clear();
	}
		
	public static void main(String[] args) throws FileNotFoundException, IOException, UnexpectedNarrativeObjectException, InterruptedException {
		LOG.info(Settings.i().get("VERSION"));
		String[] narratives = { "http://localhost/ShapeShift/rs/owl/ta2myvideos.production.2.owl" };
		OntologyInterface ow = new Wrapper(narratives, null);
		NslInterpreter i = new NslInterpreter(ow);
		i.interpret();
	}
}
