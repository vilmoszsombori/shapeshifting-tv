package tv.shapeshifting.nsl.rules;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.ArithmeticExpressionParser;
import tv.shapeshifting.nsl.exceptions.ArithmeticExpressionEvaluationException;
import tv.shapeshifting.nsl.ontology.SparqlFileRepository;

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

public class evaluateArithmeticExpression extends BaseBuiltin {

	static Logger LOG = Logger.getLogger(evaluateArithmeticExpression.class);

	public String getName() {
        return "evaluateArithmeticExpression";
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

		String queryString;
		try {
			queryString = SparqlFileRepository.i().get("queries/getContextVariables.query");
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
        
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
			//env.bind(args[length - 1], Node.createLiteral(Double.valueOf(val)));
			Node n = infModel.createTypedLiteral(val).asNode();
			env.bind(args[length - 1], n /* Node.createLiteral(String.valueOf(val)) */);
			return true;
		} catch (ArithmeticExpressionEvaluationException e) {
			LOG.warn(e.get() + ". Expression: [" + args[0].toString(false) + "]");
			return false;
		}				
	}

	public void headAction(Node[] args, int length, RuleContext context) {
		LOG.warn("This method is not expected to be called.");
	}
			
}



