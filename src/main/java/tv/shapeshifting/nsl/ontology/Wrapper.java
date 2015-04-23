package tv.shapeshifting.nsl.ontology;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import tv.shapeshifting.nsl.ArithmeticExpressionParser;
import tv.shapeshifting.nsl.MediaTypeMap;
import tv.shapeshifting.nsl.OntologyInterface;
import tv.shapeshifting.nsl.Settings;
import tv.shapeshifting.nsl.exceptions.ArithmeticExpressionEvaluationException;
import tv.shapeshifting.nsl.exceptions.TimecodeFormatException;
import tv.shapeshifting.nsl.exceptions.UnexpectedNarrativeObjectException;
import tv.shapeshifting.nsl.exceptions.UnrecognizedDatatypeException;
import tv.shapeshifting.nsl.exceptions.UnrecognizedMediatypeException;
import tv.shapeshifting.nsl.functions.formatUri;
import tv.shapeshifting.nsl.functions.uuid;
import tv.shapeshifting.nsl.util.Timecode;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.impl.OntResourceImpl;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.FileManager;

public class Wrapper implements OntologyInterface {
	
	/* ***********************************************
	 * Public constructor called from EngineDispatcher
	 * *********************************************** 
	 */

	public Wrapper(String[] owls, String[] rules) throws FileNotFoundException, IOException {
		// set up the RDF namespaces
		if ( Settings.i().get("NSL_STATIC_RULES") != null )
			this.rules.add(Settings.i().get("NSL_STATIC_RULES"));
		if ( Settings.i().get("NSL_DYNAMIC_RULES") != null )
			this.rules.add(Settings.i().get("NSL_DYNAMIC_RULES"));
		
		boolean transitiveClosure = Boolean.getBoolean(Settings.i().get("TRANSITIVE_CLOSURE", "false"));
		
		// initialise static ontology model
		LOG.info("Initialise static ontology model...");
		
		// initialise raw model
		rawModel = createOntologyModel("none", null);

		// add CORE NSL model
		addModel(rawModel, Settings.i().get("NSL_BASE_MODEL"));

		// add PRODUCTION models
		for ( int i = 0; i < owls.length; i++) {
			LOG.debug("Adding production model [" + owls[i] + "]...");
			addModel(rawModel, owls[i]);
			LOG.debug("Model [" + owls[i] +"] added. Raw model size = " + getRawModel().size());
		}
		
		// Add rules
		if ( rules != null )
			for ( int i = 0; i < rules.length; i++ )
				this.rules.add(rules[i]);
		else
			LOG.debug("No rules provided.");
		
		
		// register external functions with the reasoner
		//BuiltinRegistry.theRegistry.register(new canFollow());
		//BuiltinRegistry.theRegistry.register(new objectValue());

		// TODO The definition of the used external functions should be in the .rules
		BuiltinRegistry.theRegistry.register(new tv.shapeshifting.nsl.rules.evaluateArithmeticExpression());
		BuiltinRegistry.theRegistry.register(new tv.shapeshifting.nsl.rules.castAsNumber());	
		BuiltinRegistry.theRegistry.register(new tv.shapeshifting.nsl.rules.timecode());	
		BuiltinRegistry.theRegistry.register(new tv.shapeshifting.nsl.rules.sparql());	

		/*
		 *  add static inference rules to the raw model
		 *  these subsequently will be added to the 
		 *  ontology model that is based on the raw model
		 */
		for ( int i = 0; i < this.rules.size(); i++ ) {
			String ruleURL = this.rules.get(i).trim();
			if ( ruleURL.toLowerCase().endsWith("static.rule") ) {
				InfModel tempModel = addRules(rawModel, ruleURL, transitiveClosure);
				Model tempModel1 = tempModel.difference(rawModel);
				rawModel.addSubModel(tempModel1);				
			}
		}

		ontModel = createOntologyModel(Settings.i().get("REASONING_LEVEL", "owl").toLowerCase(), rawModel);
		LOG.debug("Static ontology model initialised [" + getOntModel().size() + "]");

		// initialise dynamic inference model
		LOG.info("Initialize dynamic inference model...");		
		int nr = applyStaticRules(transitiveClosure);		
		LOG.debug("Dynamic inference model initialised using [" + nr + "] static rule files; size = [" + getInfModel().size() + "]");
	}

	private int applyStaticRules(boolean transitive) {
		int i;
		for ( i = 0; i < rules.size(); i++ ) {
			String ruleURL = rules.get(i).trim();
			if ( ruleURL.toLowerCase().endsWith("static.rule") )
				try {
					addRules(ruleURL, transitive);
				} catch (MalformedURLException e) {
					LOG.warn("Can't load rule(s) [" + ruleURL + "]. Exception: " + e.getMessage());
				}				
		}
		return i;
	}

	private InfModel addRules(OntModel model, String url, boolean transitive) throws MalformedURLException {
		LOG.debug("Adding rules [" + url + "]...");
		URL rulesURL = new URL(url);		
		List<Rule> rules = Rule.rulesFromURL(rulesURL.toString());		
		GenericRuleReasoner rulesReasoner = new GenericRuleReasoner(rules);
		
		/* TODO I'm not so sure about the following two statements:
		 * * rulesReasoner.setOWLTranslation(true);
		 * * rulesReasoner.setTransitiveClosureCaching(true);
		 * Reasoner rulesReasoner = PelletReasonerFactory.theInstance().create();
		 */
		
		if(transitive)
			rulesReasoner.setTransitiveClosureCaching(true);
		InfModel infModel = ModelFactory.createInfModel(rulesReasoner, model);
	
		//model.add(infModel.difference(model));
		//model.add(infModel);
		return infModel;
	}
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#addRules(java.lang.String, boolean)
	 */
	@Override
	public void addRules(String url, boolean transitive) throws MalformedURLException {
		LOG.debug("Adding rules [" + url + "] ...");
		URL rulesURL = new URL(url);		
		List<Rule> rules = Rule.rulesFromURL(rulesURL.toString());	
		GenericRuleReasoner rulesReasoner = new GenericRuleReasoner(rules);
		// TODO I'm not so sure about the following two statements
		//rulesReasoner.setOWLTranslation(true);
		//rulesReasoner.setTransitiveClosureCaching(true);
		//Reasoner rulesReasoner = PelletReasonerFactory.theInstance().create();
		if(transitive)
			rulesReasoner.setTransitiveClosureCaching(true);

		long before = infModel == null ? 0 : infModel.size(), after = before;
		
		// Check if the inference model has been created. If not, create it.
		if ( infModel == null ) {
			// Make it concurrent			
			getRawModel().enterCriticalSection(Lock.READ);
			//getOntModel().enterCriticalSection(Lock.READ);
			try {
				infModel = ModelFactory.createInfModel(rulesReasoner, getRawModel());
				//infModel.add(getOntModel());
				//infModel = ModelFactory.createInfModel(rulesReasoner, getOntModel());
				after = infModel.size();
			} finally {
				getRawModel().leaveCriticalSection();
				//getOntModel().leaveCriticalSection();
			}			
		} else {
			// Make it concurrent
			getInfModel().enterCriticalSection(Lock.WRITE);
			try {
				InfModel _infModel = ModelFactory.createInfModel(rulesReasoner, getInfModel());
				getInfModel().add(_infModel);
				after = getInfModel().size();
			} finally {
				getInfModel().leaveCriticalSection();
			}
		}
	
		rules = rulesReasoner.getRules();
		for	(Iterator<Rule> it = rules.iterator(); it.hasNext(); ) {
			LOG.debug(it.next().toString());
		}
		LOG.info((after - before) + " new RDF triples added. There are " + after + " RDF triples in total.");
		
		//model.add(infModel.difference(model));
		//model.add(infModel);
	}		

	private OntModel createOntologyModel(String reasoningLevel, Model base) {
		LOG.debug("Initializing model with " + reasoningLevel + " reasoning level...");
		
		//create the appropriate Jena model
		OntModel model = null;
		OntModelSpec spec = null;
		
		if("none".equals(reasoningLevel.toLowerCase())) {
			/*
			 * "none" is jena model with OWL_DL
			 * ontologies loaded and no inference enabled
			 */
			spec = OntModelSpec.OWL_DL_MEM;
		} else if("rdfs".equals(reasoningLevel.toLowerCase())) {
	    	/*
			 * "rdfs" is jena model with OWL_DL
			 * ontologies loaded and RDFS inference enabled 
			 */
			spec = OntModelSpec.OWL_DL_MEM_RDFS_INF;
	    } else if("owl".equals(reasoningLevel.toLowerCase())) {
	    	/*
	    	 * "owl" is jena model with OWL_DL ontologies
	    	 * wrapped around a pellet-based inference model
	    	 */
	    	spec = PelletReasonerFactory.THE_SPEC;	    	
	    } else {
	    	//invalid inference setting
	    	LOG.warn("Invalid inference setting, choose one of <none|rdfs|owl>.\nModel is null!");
	    }		
    	if(base == null) {
    		model = ModelFactory.createOntologyModel(spec);
    	} else {
    		model = ModelFactory.createOntologyModel(spec, base);	    		
    	}

    	model.prepare();
    	
		return model;
	}

	private void addModel(OntModel model, String url) throws MalformedURLException {
		LOG.info("Loading model " + url + " ...");
		URL inputModelURL = new URL(url);
		Model inputModel = url.toUpperCase().trim().endsWith("TTL") ? 
				FileManager.get().loadModel(inputModelURL.toString(), "TTL") : 
					FileManager.get().loadModel(inputModelURL.toString());
		model.addSubModel(inputModel);		
	}
	
	
	/* **************************
	 * Called from NslInterpreter
	 * **************************
	 */

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getNarrativeRoot()
	 */
	@Override
	public Individual[] getNarrativeRoot() throws IOException {
		String queryString = SparqlFileRepository.i().get("queries/narrativeRoot.query");
		Query query = QueryFactory.create(queryString);
		getRawModel().enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, getRawModel());
			ResultSet results = qexec.execSelect();
			try {
				ArrayList<Individual> res = new ArrayList<Individual>();
				while (results.hasNext()) {
					QuerySolution solution = results.nextSolution();
					RDFNode root = solution.get("root");
					Individual i = getOntModel().getIndividual(root.toString());
					res.add(i);
				}
				Individual[] ret = new Individual[res.size()];
				return res.toArray(ret);
			} finally {
				qexec.close();
			}
		} finally {
			getRawModel().leaveCriticalSection();
		}
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#preprocessSparqlExpressions()
	 */
	@Override
	public void preprocessSparqlExpressions() throws IOException {
		String queryString = SparqlFileRepository.i().get("queries/sparqlExpressions.query");
		Query query = QueryFactory.create(queryString);
		getRawModel().enterCriticalSection(Lock.READ);
		getInfModel().enterCriticalSection(Lock.WRITE);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, getRawModel());
			ResultSet results = qexec.execSelect();
			try {
				while (results.hasNext()) {
					QuerySolution solution = results.nextSolution();
					RDFNode root = solution.get("sparql");
					Query innerQuery = QueryFactory.create(root.toString());
					QueryExecution innerQexec = QueryExecutionFactory.create(innerQuery, getRawModel());
					Model model = innerQexec.execConstruct();
					if ( model.size() > 0 ) {
						// add results to the inference model
						getInfModel().add(model);
						LOG.debug("Preprocess SPARQL expressions of implicit objects: [" + model.size() + "] tripples added to inference model. [" + getInfModel().size() + "] triples in total.");
					}
				}
			} finally {
				qexec.close();
			}
		} finally {
			getRawModel().leaveCriticalSection();
			getInfModel().leaveCriticalSection();
		}		
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#preprocessContextVariables()
	 */
	@Override
	public void preprocessContextVariables() throws IOException {
		LOG.debug("Launching [codeAnnotations.query] ...");
		String queryString = SparqlFileRepository.i().get("queries/codeAnnotations.query");

		HashMap<String, String> label2uri = new HashMap<String, String>();

		String updateString = "PREFIX nsl: <http://shapeshifting.tv/ontology/nsl#>\n" +
				"PREFIX production: <http://shapeshifting.tv/ontology/production#>\n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"; 
		
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, getRawModel());
		ResultSet results = qexec.execSelect();		
		
		while (results.hasNext()) {
			QuerySolution solution = results.nextSolution();
			String code = solution.getLiteral("code").getString();
			Pattern p = Pattern.compile("set\\([\\s\\']*([^\\(\\)\\,\\']+)[\\s\\']*,[\\s]*([^\\(\\)\\,]+)\\)");
			Matcher m = p.matcher(code);
	 
			while (m.find()) {
				String variableName = m.group(1); //StringFunction.toVariableName(m.group(1));
				if(!label2uri.containsKey(variableName)) {
					String uri = UUID.randomUUID().toString();
					label2uri.put(variableName, uri);
					//Literal l = getInfModel().createTypedLiteral(m.group(2));

					updateString += "INSERT DATA { production:" + uri + " a nsl:ContextVariable ; " +
							"rdfs:label \"" + variableName + "\"^^xsd:string ; " +
							"nsl:hasValue " + m.group(2) + " } ;\n"; // TODO type missing
				}
				//LOG.debug(uri + ": " + m.group(1) + " = " + m.group(2));
			}					
		} 
		
		if(!label2uri.isEmpty())
			update(updateString, getInfModel());
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#applyDynamicRules(boolean)
	 */
	@Override
	public int applyDynamicRules(boolean transitive) throws MalformedURLException {
		int i;
		for ( i = 0; i < rules.size(); i++ ) {
			String ruleURL = rules.get(i).trim();
			if ( ruleURL.toLowerCase().endsWith("dynamic.rule") )
				addRules(ruleURL, transitive);				
		}
		return i;
	}


	/* ********************************
	 * Called from NslInterpreterThread
	 * ********************************
	 */
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#isAtomic(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public boolean isAtomic(Individual individual) {
		String s = individual.getOntClass(true).toString();
		if(s.contains("AtomicNarrativeObject"))
			return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#isMediaObject(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public boolean isMediaObject(Individual individual) {
		String s = individual.getOntClass(true).toString();
		if(s.contains("MediaItem"))
			return true;
		return false;
	}
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#isImplicitObject(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public boolean isImplicitObject(Individual individual) {
		String s = individual.getOntClass(true).toString();
		if(s.contains("ImplicitObject"))
			return true;
		return false;
	}	
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#isLinkStructure(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public boolean isLinkStructure(Individual individual) {
		String s = individual.getOntClass(true).toString();
		if(s.contains("LinkStructure"))
			return true;
		return false;
	}	

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#isBinStructure(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public boolean isBinStructure(Individual individual) {
		String s = individual.getOntClass(true).toString();
		if(s.contains("BinStructure"))
			return true;
		return false;
	}	

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#isLayerStructure(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public boolean isLayerStructure(Individual individual) {
		String s = individual.getOntClass(true).toString();
		if(s.contains("LayerStructure"))
			return true;
		return false;
	}	

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#isDecisionPoint(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public boolean isDecisionPoint(Individual individual) {
		String s = individual.getOntClass(true).toString();
		if(s.contains("DecisionPoint"))
			return true;
		return false;
	}	

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#isLink(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public boolean isLink(Individual individual) {
		String s = individual.getOntClass(true).toString();
		if(s.contains("Link"))
			return true;
		return false;
	}
	

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#setBeingProcessed(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public void setBeingProcessed(Individual individual) throws IOException {		
		String s = SparqlFileRepository.i().get("updates/setBeingProcessed.update");
		String updateString = String.format(s, individual.getURI());
		update(updateString, getInfModel());		
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getPlaylistEntry(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public HashMap<String, Object> getPlaylistEntry(Individual individual) throws FileNotFoundException, IOException, TimecodeFormatException, UnrecognizedMediatypeException {
		String s = SparqlFileRepository.i().get("queries/getPlaylistEntry.query");
		String queryString = String.format(s, individual.getURI());
		Query query = QueryFactory.create(queryString);
		// TODO double check which model is to be used here
		getRawModel().enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, getRawModel());
			ResultSet results = qexec.execSelect();
			try {
				while (results.hasNext()) {
					QuerySolution solution = results.nextSolution();
					HashMap<String, Object> ret = new HashMap<String, Object>();
					RDFNode node;
					node = solution.get("src");//SELECT ?type ?src ?clipBegin ?clipEnd ?fileDuration
					if(node.isLiteral())
						ret.put("src", node.asLiteral().getString());
					node = solution.get("type");//SELECT ?type ?src ?clipBegin ?clipEnd ?fileDuration
					if(node.isLiteral()) {
						String type = node.asLiteral().getString();						
						ret.put("type", MediaTypeMap.i().get(type));
						//type = mediaTypeMap.containsKey(type) ? mediaTypeMap.get(type) : "unknown" ;
					}
					node = solution.get("clipBegin");//SELECT ?type ?src ?clipBegin ?clipEnd ?fileDuration
					if(node.isLiteral())
						ret.put("clipBegin", Timecode.parse(node.asLiteral().getString()).toSeconds());
					node = solution.get("clipEnd");//SELECT ?type ?src ?clipBegin ?clipEnd ?fileDuration
					if(node.isLiteral())
						ret.put("clipEnd", Timecode.parse(node.asLiteral().getString()).toSeconds());
					node = solution.get("fileDuration");//SELECT ?type ?src ?clipBegin ?clipEnd ?fileDuration
					if(node.isLiteral())
						ret.put("fileDuration", Timecode.parse(node.asLiteral().getString()).toSeconds());					
					// TODO move the ID conversion from here to somewhere more sensible					
					String uri = individual.getURI();
					uri = formatUri.from(uri);
					ret.put("id", uri);
					//LOG.debug("src: " + ret.get("src") + "; type = " + ret.get("type") + "; clipBegin = " + ret.get("clipBegin") + "; clipEnd = " + ret.get("clipEnd"));
					return ret;
				}
			} finally {
				qexec.close();
			}
		} finally {
			getRawModel().leaveCriticalSection();
		}
		LOG.warn("No playlist entry created for [" + individual.getLabel("") + "]");
		return null;
	}
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getInteractions(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public Vector<HashMap<String, Object>> getInteractions(Individual individual) throws TimecodeFormatException, IOException {
		String s = SparqlFileRepository.i().get("queries/getInteractions.query");
		String queryString = String.format(s, individual.getURI());
		Query query = QueryFactory.create(queryString);

		// TODO double check which model is to be used here
		getOntModel().enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, getOntModel());
			ResultSet results = qexec.execSelect();
			try {
				Vector<HashMap<String, Object>> ret = new Vector<HashMap<String, Object>>();
				while (results.hasNext()) {
					QuerySolution solution = results.nextSolution();

					HashMap<String, Object> temp = new HashMap<String, Object>();				

					if ( solution.get("label") == null ) {
						LOG.warn("No label specified for interaction [" + individual.getLabel("") + "]");
						continue;
					}
					
					String text = solution.get("label").asLiteral().getString();//?label ?action ?begin ?duration
					temp.put("text", text);
					
					if ( solution.get("duration") != null ) {
						long duration = solution.get("duration").asLiteral().getLong(); //?label ?action ?begin ?duration
						String dur = Timecode.valueOf(duration).toSeconds();
						temp.put("dur", dur);
					} else {
						LOG.warn("No duration specified for interaction [" + text + "]. Interaction will be skipped.");
						continue;
					}
					
					if ( solution.get("begin") != null ) {
						String begin = solution.get("begin").asLiteral().getString();
						temp.put("begin", begin);
					} else {
						LOG.warn("No begin time specified for interaction [" + text + "]. Interaction will be skipped.");
						continue;						
					}
					
					if ( solution.get("actionUrl") != null ) {
						String actionUrl = solution.get("actionUrl").asLiteral().getString(); //?label ?action ?begin ?duration
						//actionUrl = Settings.i().get("SERVER_URL") + "/" + actionUrl;
						temp.put("url", actionUrl);
					} else {
						LOG.warn("No actionUrl specified for interaction [" + text + "]");						
					}
										
					
					if ( solution.get("imgUrl") != null ) {
						String imgUrl = solution.get("imgUrl").asLiteral().getString(); //?label ?action ?begin ?duration
						//imgUrl = Settings.i().get("SERVER_URL") + "/" + imgUrl;
						temp.put("img", imgUrl);
					} else {
						LOG.warn("No imgUrl specified for interaction [" + text + "]");
					}					

					if(!temp.isEmpty()) {
						temp.put("id", uuid.generate());
						//LOG.debug("Interaction: " + temp.get("text") + " | " + temp.get("actionUrl"));
						ret.add(temp);
					}
				}
				return ret;
			} finally { qexec.close(); }
		} finally { getOntModel().leaveCriticalSection(); }
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#updateStructuredObjectTiming(com.hp.hpl.jena.ontology.Individual, com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public void updateStructuredObjectTiming(Individual parent, Individual individual) throws IOException {
		String s = SparqlFileRepository.i().get("updates/structuredObjectTiming.update");
		String updateString = String.format(s, parent.getURI(), individual.getURI());
		update(updateString, getInfModel());		
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getMediaObject(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public Individual getMediaObject(Individual individual) throws UnexpectedNarrativeObjectException {
		if(!isAtomic(individual)) {
			String s = individual.getOntClass(true).toString();
			s = s.substring(s.indexOf('#') + 1);			
			throw new UnexpectedNarrativeObjectException(s + " found. Atomic object expected.");
		}
		RDFNode r = individual.getPropertyValue(getRawModel().getProperty(NSL_NAMESPACE + "refersTo"));
		if(r != null) {
			OntResource o = (OntResource)r;
			return o.asIndividual();
		} else {
			LOG.warn("No reference to Media object found [" + individual.getURI() + "]");
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getStartItemOf(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public Individual getStartItemOf(Individual individual) throws UnexpectedNarrativeObjectException {
		if(!isLinkStructure(individual)) {
			String s = individual.getOntClass(true).toString();
			s = s.substring(s.indexOf('#') + 1);			
			throw new UnexpectedNarrativeObjectException(s + " found. LinkStructure expected.");
		}
		RDFNode r = individual.getPropertyValue(getRawModel().getProperty(NSL_NAMESPACE + "hasStartItem"));
		if(r != null) {
			OntResource o = (OntResource)r;
			return o.asIndividual();
		} else {
			LOG.warn("Missing start item!");
			NodeIterator i = individual.listPropertyValues(getRawModel().getProperty(NSL_NAMESPACE + "hasNarrativeItem"));
			while(i.hasNext()) {
	        	OntResourceImpl ni = (OntResourceImpl) i.next();
	        	Individual ind = ni.asIndividual();
	        	return ind;
			}			
		}
		LOG.error("Empty link structure!");
		return null;
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getNextLinkStructureItem(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public Individual[] getNextLinkStructureItem(Individual individual) throws IOException {
		String s = SparqlFileRepository.i().get("queries/getNextLinkStructureItem.query");
		String queryString = String.format(s, individual.getURI());
		Query query = QueryFactory.create(queryString);
		
		ArrayList<Individual> result = new ArrayList<Individual>();
		getInfModel().enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, getInfModel());
			ResultSet results = qexec.execSelect();	
			try {
				while (results.hasNext()) {
					QuerySolution solution = results.nextSolution();
					RDFNode root = solution.get("no");
					Individual i = getOntModel().getIndividual(root.toString());
					root = solution.get("condition");
					if ( root != null && root.isLiteral() ) {
						LOG.debug("Link condition: " + root.asLiteral().getString());
					}
					root = solution.get("value");
					if ( root != null && root.isLiteral() ) {
						LOG.debug("Link value: " + root.asLiteral().getDouble());
					}
					result.add(i);
				}		
			} finally {
				qexec.close();
			}
			Individual[] res = new Individual[result.size()];
			return result.toArray(res);
		} finally {
			getInfModel().leaveCriticalSection();
		}
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getSelectType(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public OntologyInterface.SelectType getSelectType(Individual individual) throws UnexpectedNarrativeObjectException {
		if(!isBinStructure(individual) && !isImplicitObject(individual)) {
			String s = individual.getOntClass(true).toString();
			s = s.substring(s.indexOf('#') + 1);			
			throw new UnexpectedNarrativeObjectException(s + " found. BinStructure or ImplicitObject expected.");
		}
		RDFNode r = individual.getPropertyValue(getRawModel().getProperty(NSL_NAMESPACE + "hasSelectType"));
		if(r != null) {
			String s = r.toString();
			s = s.substring(s.indexOf('#') + 1);			
			//LOG.debug("r: " + r + ", s: " + s);
			return s.equals(OntologyInterface.SelectType.SELECT_SEQUENCE.toString()) ? OntologyInterface.SelectType.SELECT_SEQUENCE : 
				(s.equals(OntologyInterface.SelectType.SELECT_ALTERNATIVES.toString()) ? OntologyInterface.SelectType.SELECT_ALTERNATIVES : OntologyInterface.SelectType.SELECT_ONE );
		} else {
			LOG.warn("No selectType specified. SelectOne is chosen by default!");
			return OntologyInterface.SelectType.SELECT_ONE;
		}
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getBinItems(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public Individual[] getBinItems(Individual individual) throws IOException {
		RDFNode r = individual.getPropertyValue(getOntModel().getProperty(NSL_NAMESPACE + "hasSelectionRule"));
		String queryString = null;
		if(r != null && r.isLiteral() && !r.toString().isEmpty()) {
			String temp = r.toString().trim();
			if ( temp.startsWith("sparql(") && temp.endsWith(")") ) {
				temp = temp.substring(7, temp.length() - 1);
				//LOG.debug("SPARQL selection rule: " + temp);
				queryString = String.format(temp, individual.getURI());
			} else if ( temp.startsWith("select") ) {
				// Check if the selection rule is old style select(), default(), alt()
				LOG.warn("NSL1.0 style selection rule is being skipped: " + temp);				
			} else if ( temp.startsWith("FILTER") ) {
				LOG.debug("Filter selection rule: " + temp);
				String s = SparqlFileRepository.i().get("queries/getBinItems.query");
				queryString = String.format(s, individual.getURI(), temp);					
			}				
		}
		
		if ( queryString == null ) {
			String s = SparqlFileRepository.i().get("queries/getBinItems.query");
			queryString = String.format(s, individual.getURI(), "");					
		}

		Query query = null;
		
		try {
			query = QueryFactory.create(queryString);
		} catch (Exception e) {
			LOG.error("Invalid selection rule: " + queryString);
			e.printStackTrace();
			return new Individual[0];
		} 
		
		// TODO double check which model is to be used here
		//getInfModel().enterCriticalSection(Lock.READ);
		getOntModel().enterCriticalSection(Lock.READ);
		try {
			//QueryExecution qexec = QueryExecutionFactory.create(query, getInfModel());
			QueryExecution qexec = QueryExecutionFactory.create(query, getOntModel());
			ResultSet results = qexec.execSelect();
			try {
				ArrayList<Individual> res = new ArrayList<Individual>();
				while (results.hasNext()) {
					QuerySolution solution = results.nextSolution();
					RDFNode root = solution.get("binItem");
					Individual i = getOntModel().getIndividual(root.toString());
					res.add(i);
				}
				Individual[] ret = new Individual[res.size()];
				return res.toArray(ret);
			} finally {
				qexec.close();
			}
		} finally {
			getOntModel().leaveCriticalSection();
			//getInfModel().leaveCriticalSection();
		}
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#evaluateTerminationCondition(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public boolean evaluateTerminationCondition(Individual individual) throws IOException {
		RDFNode r = individual.getPropertyValue(getInfModel().getProperty(NSL_NAMESPACE + "hasTerminationCondition"));

		if ( r != null && r.isLiteral() && !r.toString().isEmpty()) {
			String terminationCondition = r.toString().trim();
			
			if ( terminationCondition.startsWith("sparql(") && terminationCondition.endsWith(")") ) {
				terminationCondition = terminationCondition.substring(7, terminationCondition.length() - 1);
				boolean val = ask(terminationCondition, getInfModel());
				LOG.debug("SPARQL termination condition has evaluated to [" + val + "]. " + (val ? "Looping..." : "Not looping."));
				return val;
			} else if ( terminationCondition.startsWith("condition(") && terminationCondition.endsWith(")") ) {
				terminationCondition = terminationCondition.substring(10, terminationCondition.length() - 1);
				// Refresh context variables
				Map<String, Double> contextVariables = getContextVariables(getInfModel());				
				ArithmeticExpressionParser aep = new ArithmeticExpressionParser();
				aep.updateUserVariables(contextVariables);
				double val;
				try {
					val = aep.parse(terminationCondition);
					boolean ret = (val > 0);
					LOG.debug("Termination condition [" + terminationCondition + "] has evaluated to [" + val + "]. " + (ret ? "Looping..." : "Not looping."));
					return ret;
				} catch (ArithmeticExpressionEvaluationException e) {
					LOG.warn(e.get() + ". Expression: [" + terminationCondition + "]");
				}
			} else {
				LOG.warn("Termination condition [" + terminationCondition + "] has an unexpected format.");				
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getLeadingLayerOf(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public Individual getLeadingLayerOf(Individual individual) throws UnexpectedNarrativeObjectException {
		if(!isLayerStructure(individual)) {
			String s = individual.getOntClass(true).toString();
			s = s.substring(s.indexOf('#') + 1);			
			throw new UnexpectedNarrativeObjectException(s + " found. LayerStructure expected.");
		}
		RDFNode r = individual.getPropertyValue(getRawModel().getProperty(NSL_NAMESPACE + "hasLeadingLayer"));
		if(r != null) {
			OntResource o = (OntResource)r;
			return o.asIndividual();
		} else {
			LOG.warn("No leading layer specified.");
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getNarrativeItemsOf(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public Individual[] getNarrativeItemsOf(Individual individual) throws UnexpectedNarrativeObjectException, IOException {
		if(!isStructured(individual)) {
			String s = individual.getOntClass(true).toString();
			s = s.substring(s.indexOf('#') + 1);			
			throw new UnexpectedNarrativeObjectException(s + " found. Structured narrative object expected.");
		}
		ArrayList<Individual> res = new ArrayList<Individual>();
		NodeIterator i = individual.listPropertyValues(getRawModel().getProperty(NSL_NAMESPACE + "hasNarrativeItem"));
		while(i.hasNext()) {
			RDFNode node = i.next();
			Individual ind = getOntModel().getIndividual(node.toString());
			res.add(ind);			
		}
		Individual[] ret = new Individual[res.size()];
		return res.toArray(ret);
	}

	private boolean isStructured(Individual individual) throws IOException {		
		String s = SparqlFileRepository.i().get("asks/isStructured.ask");
		String askString = String.format(s, individual.getURI());
		return ask(askString, getOntModel());
	}
	
	private Map<String, Double> getContextVariables(Model model) throws IOException {
		Map<String, Double> contextVariables = new HashMap<String, Double>();		
		String queryString = SparqlFileRepository.i().get("queries/getContextVariables.query");
		Query query = QueryFactory.create(queryString);
		model.enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			ResultSet results = qexec.execSelect();
			try {
				while (results.hasNext()) {
					QuerySolution solution = results.nextSolution();
					String label = solution.getLiteral("label").getString();
					Object value = solution.getLiteral("value").getValue();
					if(value instanceof Double)
						contextVariables.put(label, (Double)value);
					else if(value instanceof Integer)
						contextVariables.put(label, Double.valueOf(value.toString()));
					else
						LOG.warn("Unrecognized type of variable: " + label + " = " + value + "; [" + solution.getLiteral("value").getDatatypeURI() + "]");
				}
				return contextVariables;
			} finally {
				qexec.close();
			}
		} finally {
			model.leaveCriticalSection();
		}		
	}
	

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#updateDuration(com.hp.hpl.jena.ontology.Individual, long)
	 */
	@Override
	public void updateDuration(Individual individual, long duration) throws IOException {
		String s = SparqlFileRepository.i().get("updates/duration.update");
		String updateString = String.format(s, individual.getURI(), duration);
		update(updateString, getInfModel());				
	}
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#hasPlaylistBarrier(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public boolean hasPlaylistBarrier(Individual individual) throws IOException {
		String s = SparqlFileRepository.i().get("asks/hasPlaylistBarrier.ask");
		String askString = String.format(s, individual.getURI());
		return ask(askString, getOntModel());
	}	
	

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#setHasBeenProcessed(com.hp.hpl.jena.ontology.Individual)
	 */
	@Override
	public void setHasBeenProcessed(Individual individual) throws IOException {
		String s = SparqlFileRepository.i().get("updates/setHasBeenProcessed.update");
		String updateString = String.format(s, individual.getURI());
		update(updateString, getInfModel());		
	}

	/* ***************************
	 * Called from SessionListener
	 * ***************************
	 */

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#closeModels()
	 */
	@Override
	public void closeModels() {
		LOG.debug("Closing inference model...");
		getInfModel().close();
		LOG.debug("Closing ontology model...");
		getOntModel().close();
		LOG.debug("Closing raw model...");
		getRawModel().close();		
	}
		

	/* ***************************
	 * Called from QueryDispatcher
	 * ***************************
	 */

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getInfModel()
	 */
	@Override
	public InfModel getInfModel() {
		if(infModel != null)
			return infModel;
		else
			return getOntModel();
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getOntModel()
	 */
	@Override
	public OntModel getOntModel() {
		if(ontModel != null)
			return ontModel;
		else
			return getRawModel();
	}
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#getRawModel()
	 */
	@Override
	public OntModel getRawModel() {
		return rawModel;
	}
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#update(java.lang.String, com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	public void update(String updateString, Model model) {
		UpdateRequest update = UpdateFactory.create(updateString);
		model.enterCriticalSection(Lock.WRITE);
		try {
			UpdateAction.execute(update, model);
		} finally {
			model.leaveCriticalSection();
		}
	}
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#construct(java.lang.String, com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	public long construct(String constructString, Model model) {
		Model temp = null;
		Query query = QueryFactory.create(constructString);
		model.enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			temp = qexec.execConstruct();
		} finally {
			model.leaveCriticalSection();
		}

		long before = model.size();
		
		if ( temp != null ) {
			model.enterCriticalSection(Lock.WRITE);
			try {
				model.add(temp);
			} finally {
				model.leaveCriticalSection();
			}
		}
		
		return model.size() - before;
	}
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#ask(java.lang.String, com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	public boolean ask(String askString, Model model) {
		Query query = QueryFactory.create(askString);
		model.enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			try {
				boolean b = qexec.execAsk();
				qexec.close();
				return b;
			} finally {
				qexec.close();
			}
		} finally {
			model.leaveCriticalSection();
		}
	}

	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#logQuery(java.lang.String, com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	public String logQuery(String queryString, Model model) {
		Query query = QueryFactory.create(queryString);
		model.enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			ResultSet results = qexec.execSelect();
			try {
				ByteArrayOutputStream outStream = new ByteArrayOutputStream();
				ResultSetFormatter.out(outStream, results);
				System.out.println(outStream.toString());
				//LOG.debug("RESULT:\n" + outStream);
				return outStream.toString();
			} finally {
				qexec.close();
			}
		} finally {
			model.leaveCriticalSection();
		}
	}
		
	
	/* *********************************
	 * Called from InteractionDispatcher
	 * *********************************
	 */
	
	/**
	 * TODO revisit the conversion and throw the following exception for unsuccessful conversions: 
	 * new UnrecognizedDatatypeException("Unable to identify XS Datatype for: " + nonCanonicalValue);
	 * 
	 * @param nonCanonicalValue
	 * @return
	 * @throws UnrecognizedDatatypeException
	 */
	public static String toCanonicalValue(String nonCanonicalValue) {
		Object value;
		String xsdType;
		try { // Try casting it as integer
			value = Integer.valueOf(nonCanonicalValue);
			xsdType = "xsd:integer"; 							
		} catch(NumberFormatException nfe1) {
			try { // Try casting it as double
				value = Double.valueOf(nonCanonicalValue);
				xsdType = "xsd:double"; 															
			}  catch(NumberFormatException nfe2) { 
				try { // Try casting it as boolean
					String valueUpper = nonCanonicalValue.toUpperCase();
					if (valueUpper.equals("TRUE") || valueUpper.equals("FALSE")) {
						value = Boolean.valueOf(nonCanonicalValue);
						xsdType = "xsd:boolean";
					} else {
						// It couldn't be casted as anything so treat is as a string
						value = nonCanonicalValue; 
						xsdType = "xsd:string"; 																																
					}
				}  catch(NumberFormatException nfe3) { 
					// It couldn't be casted as anything so treat is as a string
					value = nonCanonicalValue; 
					xsdType = "xsd:string"; 																						
				}
			}
		}		
		return "\"" + value + "\"^^" + xsdType;
	}
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#isContextVariableDefined(java.lang.String)
	 */
	@Override
	public boolean isContextVariableDefined(String label) throws IOException {
		String s = SparqlFileRepository.i().get("asks/isContextVariableDefined.ask");
		String queryString = String.format(s, label);
		//LOG.debug(queryString);		
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, getInfModel()); // TODO check which model is to be used!
		boolean result = qexec.execAsk();
		//LOG.debug("Context varaible [" + label + "] is " + (result ? "" : "not ") + "defined.");
		return result;
	}
	
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#defineUntypedContextVariable(java.lang.String, java.lang.Object)
	 */
	@Override
	public long defineUntypedContextVariable(String label, Object value) throws IOException {
		String s = SparqlFileRepository.i().get("constructs/contextVariable.construct");
		String constructString = String.format(s, label, value.toString());
		return construct(constructString, getInfModel()); // TODO check which model is to be used!		
	}
			
	/* (non-Javadoc)
	 * @see tv.shapeshifting.nsl.ontology.OntologyInterface#setUntypedContextVariable(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setUntypedContextVariable(String label, Object value) throws IOException {
		String s = SparqlFileRepository.i().get("updates/contextVariable.update");
		String updateString = String.format(s, label, value.toString());
		update(updateString, getInfModel()); // TODO check which model is to be used!
	}		

	/* ************************
	 * Public static attributes
	 * ************************
	 */


	
	/* ******************
	 * Private attributes
	 * ******************
	 */
	
	private static Logger LOG = Logger.getLogger(Wrapper.class);
	private OntModel rawModel = null;
	private OntModel ontModel = null;	
	private InfModel infModel = null;
	private Vector<String> rules = new Vector<String>();
	
}
