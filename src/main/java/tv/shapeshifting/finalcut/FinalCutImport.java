package tv.shapeshifting.finalcut;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import tv.shapeshifting.nsl.functions.uuid;
import tv.shapeshifting.nsl.ontology.SparqlFileRepository;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.FileManager;

public class FinalCutImport {
	public static final String TTL = "TTL";
	public static final int RANDOM_MARKER_LOWER_BOUND = 5000; // = 5s
	public static final int RANDOM_MARKER_UPPER_BOUND = 15000; // = 15s
	private static Logger LOG = Logger.getLogger(FinalCutImport.class);
	private final OntModelSpec spec = PelletReasonerFactory.THE_SPEC; //OntModelSpec.OWL_DL_MEM_TRANS_INF; //
	private OntModel model = null;
	//private static final String SPARQL_REPOSITORY = "tv/ShapeShift/finalcut";
	
	private static final String SPARQL_PREAMBLE = "PREFIX nsl: <http://shapeshifting.tv/ontology/nsl#>\n"
			+ "PREFIX production: <http://shapeshifting.tv/ontology/production#>\n"
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
			+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n";
	
	private FinalCutImport() {
		// Set up an empty ontology model
		model = ModelFactory.createOntologyModel(spec);		
	}
	
	public long construct(String constructString, boolean preamble) {
		Model temp = null;		
		model.enterCriticalSection(Lock.READ);
		try {
			Query query = QueryFactory.create(preamble ? SPARQL_PREAMBLE
					+ constructString : constructString);
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			temp = qexec.execConstruct();
		} catch( com.hp.hpl.jena.query.QueryParseException e ) {
			LOG.error(e.getMessage());
			LOG.error(constructString);
			e.printStackTrace();
		} finally {
			model.leaveCriticalSection();
		}
		if (temp != null) {
			//LOG.debug(temp.size() + " RDF triples created. Adding them to main model...");
			model.enterCriticalSection(Lock.WRITE);
			try {
				model.add(temp);
				return temp.size();
			} finally {
				model.leaveCriticalSection();
			}
		} else {
			return 0;
		}
	}
	
	public long update(String updateString, boolean preamble) {
		UpdateRequest update = UpdateFactory.create(preamble ? SPARQL_PREAMBLE
				+ updateString : updateString);
		long before = model.size();
		model.enterCriticalSection(Lock.WRITE);
		try {
			UpdateAction.execute(update, model);
		} finally {
			model.leaveCriticalSection();
		}
		return model.size() - before;
	}
	
	public static String transform(String source, String xslt) throws TransformerException {
		StreamSource streamSource = new StreamSource(source);
		StringWriter sw = new StringWriter();
		StreamResult streamResult = new StreamResult(sw);	

		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer(new StreamSource(xslt));
		transformer.transform(streamSource, streamResult);
		
		return sw.toString();
	}
	

	public static void flush(Model model, String outputFileName) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(outputFileName);
		LOG.debug("Writing [" + model.size() + "] RDF triples to [" + outputFileName + "] ...");
		model.write(writer, FinalCutImport.TTL);
	}
	
	public void flush(String outputFileName) throws FileNotFoundException {
		FinalCutImport.flush(model, outputFileName);
	}
	
	private Model asModel(String ttlString) {
		LOG.debug("Creating model from TTL string...");
		InputStream in = new ByteArrayInputStream(ttlString.getBytes());
		Model model = ModelFactory.createOntologyModel(spec);
		model.read(in, null, "TTL");
		return model;
	}
	
	public void cleanup() {
		if ( model != null ) {
			model.close();
		}
	}	
	
	public boolean addModel(String url) {
		LOG.info("Loading model " + url + " ...");
		Model inputModel = null;
		try {
			URL inputModelURL = new URL(url);
			inputModel = url.toUpperCase().trim().endsWith("TTL") ? 
					FileManager.get().loadModel(inputModelURL.toString(), "TTL") : 
						FileManager.get().loadModel(inputModelURL.toString());
		} catch (MalformedURLException e) {
			inputModel = url.toUpperCase().trim().endsWith("TTL") ? 
					FileManager.get().loadModel(url, "TTL") : 
						FileManager.get().loadModel(url);
		}
		if ( inputModel != null ) {
			model.addSubModel(inputModel);
			LOG.debug(inputModel.size() + " RDF triples added.");
			return true;
		} else {
			LOG.warn(url + " could not be loaded.");
			return false;
		}
	}
		
	private Model importMediaContent(String source) throws TransformerException {
		String ttlString = transform(source, "./war/rs/xsl/fc2mediacontent.xsl");
		return asModel(ttlString);
	}	
	
	public void doImport(String source) throws TransformerException, FileNotFoundException {
		Model content = importMediaContent(source);
		this.model.add(content);		
	}
	
	protected String load(String url) throws IOException {
		StringBuffer s = new StringBuffer();
		BufferedReader in = new BufferedReader(
				new FileReader(url));
		String inputLine;
        while ((inputLine = in.readLine()) != null) 
        	s.append(inputLine + "\n");
        in.close();
	    return s.toString();		
	}
	
	private long createMediaInstances() throws IOException {
		long before = 0, count = 0;
		String constructString;
		
		// off-timeline media items
		before = model.size();		
		LOG.debug("Creating off-timeline media instances...");
		constructString = SparqlFileRepository.i().get("mediaInstances.offtimeline.construct");
		//constructString = load("./WebContent/rs/query/mediaInstances.offtimeline.construct");
		construct(constructString, false);
		count += model.size() - before;		
		LOG.debug((model.size() - before) + " media instances created.");

		// timeline media items
		before = model.size();		
		LOG.debug("Creating timeline media instances...");
		constructString = SparqlFileRepository.i().get("mediaInstances.construct");
		//constructString = load("./WebContent/rs/query/mediaInstances.construct");
		construct(constructString, false);
		count = model.size() - before;		
		LOG.debug(count + " media instances created.");

		/*
		LOG.debug("Creating video instances...");
		constructString = SparqlFileRepository.i().get(SPARQL_REPOSITORY + "/mediaInstances.video.construct");
		//String constructString = load("./WebContent/rs/query/mediaInstances.video.construct");
		construct(constructString, false);

		LOG.debug("Creating audio instances...");
		constructString = SparqlFileRepository.i().get(SPARQL_REPOSITORY + "/mediaInstances.audio.construct");
		//constructString = load("./WebContent/rs/query/mediaInstances.audio.construct");
		construct(constructString, false);
		*/
		return count;
	}
	
	private void importContentAnnotation(String url) throws IOException {
		LOG.debug("Import content annotations...");
		long before = model.size();
		long tCount = 0, lCount = 0;
		//StringBuffer s = new StringBuffer();
		BufferedReader in = new BufferedReader(new FileReader(url));
		String inputLine, constructString = null;
        while ((inputLine = in.readLine()) != null) {
        	String[] col = inputLine.split("\t");
        	if ( col.length == 3 ) {
        		//LOG.debug(col[0] + " | " + col[1] + " | " + col[2]);
        		if ( col[2].contains("(") )
        			col[2] = col[2].substring(0, col[2].indexOf("("));
				constructString = 
						"CONSTRUCT { " +
								"?content nsl:" + col[1] + " ?annotation " +
						"} WHERE { " +
								"?content a nsl:MediaContent ; " +
									"rdfs:label ?contentLabel . " +
								"FILTER regex(?contentLabel, \"" + col[0] + "\", \"i\") . " +
								"?annotation a nsl:LogicalEntity ; " +
									"rdfs:label ?annotationLabel . " +
								"FILTER regex(?annotationLabel, \"" + col[2] + "\", \"i\") . }";
				tCount = construct(constructString, true);
				if ( tCount == 0 ) {
					LOG.warn(col[0] + " | " + col[1] + " | " + col[2]);
				}
				lCount++;
        	}
        	//s.append(inputLine + "\n");
        }
        long after = model.size();
        in.close();
        LOG.debug((after - before) + " triples have been created. [" + lCount + "]");
	}
	
	protected void createTimelineAnnotations() throws IOException {
		LOG.debug("Creating timeline annotation...");

		Pattern p = Pattern.compile("[\\s\\']*([^;\\']*)[\\s\\']*=[\\s\\']*([^;\\']*)[\\s\\']*[;]*");
		Map<String, String> annotation = new HashMap<String, String>();
		Map<String, Long> start = new HashMap<String, Long>();
		String constructString = "", category, instance;

		String queryString = SparqlFileRepository.i().get("markers.query");
		//String queryString = load("./WebContent/rs/query/markers.query");
		
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ResultSet results = qexec.execSelect();
		while ( results.hasNext() ) {
			QuerySolution solution = results.nextSolution();
			long time = solution.getLiteral("time").getLong();
			String label = solution.getLiteral("label").getString();

			LOG.debug("Marker [" + time + ", " + label + "]");

			Matcher m = p.matcher(label);
	 
			while (m.find()) {
				category = m.group(1);
				instance = m.group(2);
				
				if ( annotation.containsKey(category) ) {
					constructString = "";
					if ( category.toLowerCase().contains("song") ) {
						constructString = 
								"CONSTRUCT { " +
										"production:" + uuid.generate() + " rdf:type ?category , owl:NamedIndividual ; " +
											"rdfs:label \"" + annotation.get(category) + "\" ; " +
											"nsl:hasRelativeIn \"" + start.get(category) + "\"^^xsd:long ; " +
											"nsl:hasRelativeOut \"" + time + "\"^^xsd:long " +
								"} WHERE { " +
										"?category rdfs:subClassOf nsl:LogicalEntity ; " +
											"rdfs:label ?label . " +
										"FILTER( regex(?label, \"" + category + "\", \"i\") ) . }";
					} else if ( category.toLowerCase().contains("stage") ) {
						constructString = 
								"CONSTRUCT { " +
										"production:" + uuid.generate() + " rdf:type ?category , owl:NamedIndividual ; " +
											"nsl:hasRelativeIn \"" + start.get(category) + "\"^^xsd:long ; " +
											"nsl:hasRelativeOut \"" + time + "\"^^xsd:long " +
								"} WHERE { " +
										"?category rdfs:subClassOf nsl:LogicalEntity ; " +
											"rdfs:label ?label . " +
										"FILTER( regex(?label, \"" + instance + "\", \"i\") ) . }";						
					}

					if ( ! constructString.isEmpty() )
						construct(constructString, true);
					//LOG.debug(category + " : " + instance);
				}
				
				annotation.put(category, instance);
				start.put(category, time);					
			}					
		}		
	}
	
	private void createRandomMarkers() throws IOException {
		String updateString;
		long DELTA = (FinalCutImport.RANDOM_MARKER_UPPER_BOUND - FinalCutImport.RANDOM_MARKER_LOWER_BOUND) / 40;
		long AVERAGE = (FinalCutImport.RANDOM_MARKER_UPPER_BOUND + FinalCutImport.RANDOM_MARKER_LOWER_BOUND) / 2;
		Random random = new Random();

		String queryString = SparqlFileRepository.i().get("consecutiveMarkerPairs.query");
		//String queryString = load("./WebContent/rs/query/consecutiveMarkerPairs.query");
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ResultSet results = qexec.execSelect();
		while ( results.hasNext() ) {
			updateString = "";
			QuerySolution solution = results.nextSolution();
			long in = solution.getLiteral("in").getLong();
			long out = solution.getLiteral("out").getLong();
			
			//LOG.debug(in + " -> " + out);
			
			long marker = in, delta, temp;
			do {
				temp = 0; 
				delta = out - marker;
				if ( AVERAGE < delta &&  delta <= FinalCutImport.RANDOM_MARKER_UPPER_BOUND )
					temp = ((delta) / 80) * 40;
				else if ( delta > FinalCutImport.RANDOM_MARKER_UPPER_BOUND )
					temp = random.nextInt((int)DELTA) * 40 + FinalCutImport.RANDOM_MARKER_LOWER_BOUND;

				if ( temp > 0 ) {
					marker += temp;
					updateString += "INSERT DATA { production:" + uuid.generate() + " rdf:type production:Marker , owl:NamedIndividual ; " +
							" rdfs:label \"randomcut\" ; nsl:hasRelativeIn \"" + marker + "\"^^xsd:long } ; ";
					//LOG.debug(marker + " [" + temp + "]");
				}				
			} while ( temp > 0 );
			
			if ( ! updateString.isEmpty() ) {
				update(updateString, true);
			}
		} 		 		
	}
	
	public static void main(String[] args) throws TransformerException, IOException {
		//TODO update the paths
		
		FinalCutImport finalCutImport = new FinalCutImport();
		finalCutImport.addModel("./war/rs/owl/ta2myvideos2.annotations.ttl");
		finalCutImport.doImport("./war/rs/xml/MyVideos2.FinalCut.master.xml");
		
		finalCutImport.importContentAnnotation("./war/rs/xml/MyVideos2.ContentAnnotations.csv");
		
		//finalCutImport.createTimelineAnnotations();
		finalCutImport.createRandomMarkers();
		
		finalCutImport.createMediaInstances();
		finalCutImport.flush("./war/rs/owl/ta2myvideos2.content.ttl");
		finalCutImport.cleanup();		
	}

}
