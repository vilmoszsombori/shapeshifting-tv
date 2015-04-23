package tv.shapeshifting.nsl.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import tv.shapeshifting.nsl.Settings;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.FileManager;

public class PerformRules {

	static Logger logger = Logger.getLogger(PerformRules.class);
	                                               
	public static void main(String[] args) throws URISyntaxException, IOException {
	
		//logger.setLevel(Level.INFO);
		logger.info("start");
		
		// TODO Auto-generated method stub
		//load ontology and rules
		
		
		URL inputModelURL= new URL(Settings.i().get("NSL_BASE_MODEL")); 
	
		Model inputModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);//ModelFactory.createDefaultModel();
		FileManager.get().readModel(inputModel, inputModelURL.toString());

		inputModelURL= new URL(Settings.i().get("PRODUCTION_MODEL")); 	

		Model productionModel = FileManager.get().loadModel(inputModelURL.toString());		
		inputModel.add(productionModel);

		/*
		List<Rule> transitionRules = Rule.rulesFromURL(transitionRulesURL.toString());
		List<Rule> communicationDetectionRules = Rule.rulesFromURL(communicationDetectionURL.toString());
		
		//create rule reasoner
		Reasoner transitionReasoner = new GenericRuleReasoner(transitionRules);
		Reasoner communicationDetectionReasoner = new GenericRuleReasoner(communicationDetectionRules);
		
		InfModel ta2Model = ModelFactory.createInfModel(transitionReasoner, inputModel);
		InfModel communicationDetectionModel = ModelFactory.createInfModel(communicationDetectionReasoner, ta2Model);
		ta2Model.add(communicationDetectionModel.difference(ta2Model));
		*/

		//URL editingRulesURL = new URL(Settings.i().get("RULES_QUERIES_LOCATION") + "editing.rule");

		URL editingRulesURL = new URL(Settings.i().get("RULES_QUERIES_LOCATION") + "selection.rule");
		
		List<Rule> editingRules = Rule.rulesFromURL(editingRulesURL.toString());
		
		//create rule reasoner
		GenericRuleReasoner editingReasoner = new GenericRuleReasoner(editingRules);
		editingReasoner.setTransitiveClosureCaching(true);
		InfModel nslModel = ModelFactory.createInfModel(editingReasoner, inputModel);
		
		
		//sparql query:
		//Query query = QueryFactory.read(Settings.i().get("RULES_QUERIES_LOCATION") + "getMediaContent.query");
		Query query = QueryFactory.read(Settings.i().get("RULES_QUERIES_LOCATION") + "getLinkStructureAnnotations.query");
		//Query query = QueryFactory.read(Settings.i().get("RULES_QUERIES_LOCATION") + "getNarrativeObjects.query");
		QueryExecution qexec = QueryExecutionFactory.create(query, nslModel);
		ResultSet results = qexec.execSelect();
		
		
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ResultSetFormatter.out(outStream, results);
		
		logger.info("sparql res: \n" + outStream);
		
		/*
		//save result		
		logger.info("saving results...");
		URL resultModelURL = new URL(Settings.i().get("RESULT_LOCATION") + "rules_result.owl");
		nslModel.write(new FileOutputStream(new File(resultModelURL.toURI())), "RDF/XML");
		*/	
		
		logger.info("terminated");
	}

}
