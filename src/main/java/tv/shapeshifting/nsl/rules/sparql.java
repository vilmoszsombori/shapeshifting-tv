package tv.shapeshifting.nsl.rules;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;
import com.hp.hpl.jena.shared.Lock;

public class sparql extends BaseBuiltin {

	static Logger LOG = Logger.getLogger(sparql.class);

	public String getName() {
        return "sparql";
    }
    
	
	public int getArgLength() {
        return 2;
    }
	
	public boolean bodyCall(Node[] args, int length, RuleContext context) {
		String temp = "";
		for(int i = 0; i < length; i++) 
			temp += args[i] + ", ";
		if ( !temp.isEmpty() )
			temp = temp.substring(0, temp.length() - 2);
		LOG.warn(getName() + ":bodyCall is not implemented [" + length + "](" + temp + ")");
		return false;
	}
	
	public void headAction(Node[] args, int length, RuleContext context) {
		if ( length != 2 ) {
			LOG.warn(getName() + ":headAction called with [" + length + "] arguments. 2 arguments expected.");
			return;
		}
		
		String sparql = args[0].toString(false);
			
		if ( sparql.startsWith("sparql(") && sparql.endsWith(")") ) {
			sparql = sparql.substring(7, sparql.length() - 1);
		} else {
			LOG.warn(getName() + ":headAction unrecognized SPARQL expression [" + sparql + "]");
		}

		sparql = String.format(sparql, args[1].toString(false));
		
		//LOG.debug(sparql);
				
        InfGraph infGraph = context.getGraph();                
        InfModel infModel = ModelFactory.createInfModel(infGraph);
        
		Model temp = null;
		Query query = QueryFactory.create(sparql);
		infModel.enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, infModel);
			temp = qexec.execConstruct();
		} finally {
			infModel.leaveCriticalSection();
		}
		
		if ( temp != null ) {
			long before = infModel.size();

			infModel.enterCriticalSection(Lock.WRITE);
			try {
				infModel.add(temp);
			} finally {
				infModel.leaveCriticalSection();
			}
				
			LOG.debug((infModel.size() - before) + " triples created.");
		} else {
			LOG.debug("Construct produced no results!");
		}

	}
			
}