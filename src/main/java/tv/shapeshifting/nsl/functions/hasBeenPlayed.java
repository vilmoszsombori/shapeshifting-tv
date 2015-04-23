package tv.shapeshifting.nsl.functions;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.expr.ExprEvalException;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase1;
import com.hp.hpl.jena.sparql.util.FmtUtils;

public class hasBeenPlayed extends FunctionBase1 {
	private static Logger LOG = Logger.getLogger(hasBeenPlayed.class);

	public hasBeenPlayed() {
		super();
	}

	@Override
	public NodeValue exec(NodeValue v) {
		Node n = v.asNode();
        if ( ! n.isURI() )
            throw new ExprEvalException("Not a URI: " + FmtUtils.stringForNode(n)) ;
		LOG.warn("This should NOT be called for " + FmtUtils.stringForNode(n) + " .");
		return NodeValue.makeBoolean(false);
	}
}
