package tv.shapeshifting.nsl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class Settings {
	private static Logger LOG = Logger.getLogger(Settings.class);
	private static Settings instance = null;
	private PropertiesConfiguration properties = new PropertiesConfiguration();
	private static String SETTINGS_FILE = "shapeshifting.properties";

	public static final String INTERPRETER	= "interpreter";
	public static final String ONTOLOGY		= "ontology";
	public static final String SMIL			= "smil";
	public static final String SESSIONID	= "sessionid";
	public static final String INTERACTION	= "interactions";

	private Settings() {
		try {
			InputStream fileStream = Settings.class.getClassLoader().getResourceAsStream(SETTINGS_FILE);
			properties.load(fileStream);
		} catch (ConfigurationException e) {
			LOG.error(e.getMessage() + " [" + SETTINGS_FILE + "]");			
		} 
		

		/*
		 * TODO finish updating the SVN settings
		 * 
		 * updateSvnInfo();  
		 */		
	}

	public static Settings i() throws FileNotFoundException, IOException {
		if (instance == null) {
			instance = new Settings();
		}
		return instance;
	}

	public String get(String key, String defaultValue) {
		return properties.getString(key, defaultValue);
	}
	
	public String[] getStringArray(String key) {
		return properties.getStringArray(key);
	}

	public String get(String key) {
		return properties.getString(key);
	}
	
	public int getInt(String key) {
		return properties.getInt(key);
	}
	
	public double getDouble(String key) {
		return properties.getDouble(key);
	}
	
	public boolean getBoolean(String key) {
		return properties.getBoolean(key);
	}

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	public static void delete() {
		instance = null;
	}

	protected void updateSvnInfo() {
		String command = "svn info";
		String s = null;

		try {
			// using the Runtime exec method:
			Process p = Runtime.getRuntime().exec(command);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));

			// read the output from the command
			String prefix = "Last Changed ";
			boolean changed = false;
			while ((s = stdInput.readLine()) != null) {
				if (s.startsWith(prefix)) {
					s = s.substring(prefix.length(), s.length());
					String[] prop = s.split(": ");
					if (prop.length == 2) {
						String key = prop[0].trim().toUpperCase();
						String value = "$" + s + " $";
						if ( ! get(key).equals(value) ) {
							properties.setProperty(key, value);
							changed = true;
							LOG.debug(key + " = " + properties.getProperty(key) + " --> updated");
						} else {
							LOG.debug(key + " = " + properties.getProperty(key) + " --> unchanged");							
						}
					}
				}
			}
			
			if ( changed ) {
				LOG.debug("Properties file is being updated...");
				FileWriter fstream = new FileWriter(Settings.class.getClassLoader()
						.getResource(SETTINGS_FILE).getFile());
				properties.save(fstream);
			}
			
			// read any errors from the attempted command
			while ((s = stdError.readLine()) != null) {
				LOG.error("System command [" + command + "]: " + s);
			}
		} catch (IOException | ConfigurationException e) {
			LOG.error("System command [" + command + "] exception: "
					+ e.getMessage());
		}
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Settings s = Settings.i();
		System.out.println("REV: " + s.get("REV"));
		s.updateSvnInfo();
		System.out.println(Settings.class.getClassLoader()
				.getResource(SETTINGS_FILE).getFile());
	}
}
