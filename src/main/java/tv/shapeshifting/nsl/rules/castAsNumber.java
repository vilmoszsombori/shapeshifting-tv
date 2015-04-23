package tv.shapeshifting.nsl.rules;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.rulesys.BindingEnvironment;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;

public class castAsNumber extends BaseBuiltin {

	static Logger LOG = Logger.getLogger(castAsNumber.class);

	public String getName() {
        return "castAsNumber";
    }
    
	
	public int getArgLength() {
        return 0;
    }
	
	public boolean bodyCall(Node[] args, int length, RuleContext context) {
		if ( length != 2 ) {
			LOG.warn("Wrong number of arguments [" + length + "]. Expected: 2.");
			return false;
		}
		
		if ( args[0].toString(false).isEmpty() ) {
			LOG.warn("Cast called with empty string.");
			return false;			
		}

		BindingEnvironment env = context.getEnv();      
        InfGraph infGraph = context.getGraph();                

		Node n = null;
		
		try {
			int val = Integer.parseInt(args[0].toString(false));
	        InfModel infModel = ModelFactory.createInfModel(infGraph);
			n = infModel.createTypedLiteral(val).asNode();
		} catch(NumberFormatException e1) {
			try {
				long val = Long.parseLong(args[0].toString(false));
		        InfModel infModel = ModelFactory.createInfModel(infGraph);
				n = infModel.createTypedLiteral(val).asNode();
			} catch(NumberFormatException e2) {
				try {
					double val = Double.parseDouble(args[0].toString(false));
			        InfModel infModel = ModelFactory.createInfModel(infGraph);
					n = infModel.createTypedLiteral(val).asNode();
				} catch(NumberFormatException e3) {
					LOG.warn("Failed to cast [" + args[0] + "] as a number.");
					return false;					
				}							
			}			
		}
				
		if ( n != null ) {
			env.bind(args[length - 1], n);
			return true;
		} else {
			LOG.warn("Failed to cast [" + args[0] + "] as a number.");
			return false;			
		}
	}

	public void headAction(Node[] args, int length, RuleContext context) {
		LOG.warn("This method is not expected to be called.");
	}

}
