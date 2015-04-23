package tv.shapeshifting.nsl.rules;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;

public class canFollow extends BaseBuiltin {

	static Logger LOG = Logger.getLogger(canFollow.class);

	public String getName() {
        return "canFollow";
    }
    
	
	public int getArgLength() {
        return 0;
    }
	
	public boolean bodyCall(Node[] args, int length, RuleContext context) {	
		LOG.info("canFollow(" + args.length + ")");
		return false;

		//logger.setLevel(Level.INFO);
        //logger.info("entering GetLatest4VoiceActivePersons");
	/*
        BindingEnvironment env = context.getEnv();      
        InfGraph infGraph = context.getGraph();
        InfModel infModel = ModelFactory.createInfModel(infGraph);
        
        Query query = QueryFactory.read(IOConfig.IO_RULES_QUERIES_LOCATION + "get_latest_voice_active_persons.query");
		QueryExecution qexec = QueryExecutionFactory.create(query, infModel);
		ResultSet results = qexec.execSelect();
		Resource currentResource;
		Resource lastResource = null;
		int counter = 1;
		while (results.hasNext() && (counter <= 4)) {
			QuerySolution soln = results.nextSolution() ;
		    currentResource = soln.getResource("person");
			if (counter == 1) {
				env.bind(args[counter - 1], currentResource.asNode());
				counter ++;	
			} else {
				if (currentResource.equals(lastResource) == false) {
					env.bind(args[counter - 1], currentResource.asNode());
					counter ++;		
				}		
			}
			lastResource = currentResource;		
		}
		if (counter == 5) {
			return true;
		} else {
			return false; 
		}
		*/			
	}

	public void headAction(Node[] args, int length, RuleContext context) {
		LOG.info("canFollow(" + args.length + ")");				
	}
			
}



