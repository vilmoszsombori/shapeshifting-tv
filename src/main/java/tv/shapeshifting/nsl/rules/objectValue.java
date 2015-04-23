package tv.shapeshifting.nsl.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.ArithmeticExpressionParser;
import tv.shapeshifting.nsl.exceptions.ArithmeticExpressionEvaluationException;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.rulesys.BindingEnvironment;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;

public class objectValue extends BaseBuiltin {

	static Logger LOG = Logger.getLogger(objectValue.class);

	public String getName() {
        return "objectValue";
    }
    
	
	public int getArgLength() {
        return 0;
    }
	
	public boolean bodyCall(Node[] args, int length, RuleContext context) {
/*		String temp = "";
		for(int i = 0; i < length; i++) 
			temp += args[i] + ", ";
		LOG.debug("objectValue:body[" + length + "](" + temp + ")");
*/		
		Map<String, Double> contextVariables = new HashMap<String, Double>();
		
		BindingEnvironment env = context.getEnv();      
        InfGraph infGraph = context.getGraph();                
        InfModel infModel = ModelFactory.createInfModel(infGraph);

        /*
		String url = Settings.i().get("QUERIES_LOCATION") + "getContextVariables.query";
		String queryString = QueryFileManager.i().get(url);
		*/
        
        String queryString =	"PREFIX core: <http://www.ist-nm2.org/ontology/core#>\n" + 
								"PREFIX production: <http://www.ist-nm2.org/ontology/production#>\n" +
								"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
								"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
								"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n\n" +
								"	SELECT ?contextVariable ?label ?value\n" +
								"	WHERE { \n" +
								"		?contextVariable a core:ContextVariable ;\n" +
								"		rdfs:label ?label ;\n" +
								"		core:hasValue ?value\n" +
								"}";
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, infModel);
		ResultSet results = qexec.execSelect();
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
		
		ArithmeticExpressionParser aep = new ArithmeticExpressionParser();
		aep.updateUserVariables(contextVariables);
		double val;
		try {
			val = aep.parse(args[0].toString(false));
			//LOG.debug(args[0] + " -> " + val);			
			env.bind(args[length - 1], Node.createLiteral(String.valueOf(val)));
			return true;
		} catch (ArithmeticExpressionEvaluationException e) {
			LOG.warn(e.get() + ". Expression: [" + args[0].toString(false) + "]");
			/* 
			Iterator<Entry<String, Double>> iterator = aep.getUserVariables().entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<String, Double> entry = iterator.next();
				System.err.println("[" + entry.getKey() + "] -> " + entry.getValue());
			}				
			e.printStackTrace();
			*/
			return false;
		}				
	}

	public void headAction(Node[] args, int length, RuleContext context) {
		/*
		String s = "";
		for(int i = 0; i < length; i++) 
			s += args[i] + ", ";
				
		LOG.debug("objectValue:body[" + length + "](" + s + ")");
		*/
		// TODO finish
		Pattern p = Pattern.compile("set\\(([^\\(\\)\\,]+),([^\\(\\)\\,]+)\\)");
		Matcher m = p.matcher(args[0].toString());
 
		//List<String> animals = new ArrayList<String>();
		while (m.find()) {
			LOG.debug(m.group(1) + " = " + m.group(2));
			//animals.add(m.group());
		}		
	}
			
}