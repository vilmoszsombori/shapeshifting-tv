package tv.shapeshifting.nsl.rules;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.exceptions.TimecodeFormatException;
import tv.shapeshifting.nsl.util.Timecode;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.rulesys.BindingEnvironment;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;

public class timecode extends BaseBuiltin {

	static Logger LOG = Logger.getLogger(timecode.class);

	public String getName() {
        return "timecode";
    }
    
	
	public int getArgLength() {
        return 0;
    }
	
	public boolean bodyCall(Node[] args, int length, RuleContext context) {
		if ( length != 2 ) {
			LOG.warn(getName() + ": Wrong number of arguments [" + length + "]. Expected: 2.");
			return false;
		}
		
		if ( args[0].toString(false).isEmpty() ) {
			LOG.warn(getName() + ": Called with empty string.");
			return false;			
		}

		BindingEnvironment env = context.getEnv();      
        InfGraph infGraph = context.getGraph();                

		Node n = null;
		
		try {
			String time = args[0].toString(false);
			long longValue = Timecode.parse(time).longValue();
			
	        InfModel infModel = ModelFactory.createInfModel(infGraph);
			n = infModel.createTypedLiteral(longValue).asNode();
		} catch(TimecodeFormatException e) {
			LOG.warn("Cast failed on [" + args[0] + "] as a Timecode.");
			return false;					
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
		String temp = "";
		for(int i = 0; i < length; i++) 
			temp += args[i] + ", ";
		if ( !temp.isEmpty() )
			temp = temp.substring(0, temp.length() - 2);
		LOG.warn(getName() + ":headAction is not implemented [" + length + "](" + temp + ")");		
	}
			
}