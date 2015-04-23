package tv.shapeshifting.nsl.functions;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase1;
import com.hp.hpl.jena.sparql.util.FmtUtils;

public class evaluate extends FunctionBase1 {
	private static Logger LOG = Logger.getLogger(evaluate.class);

	public evaluate() {
		super();
	}

	@Override
	public NodeValue exec(NodeValue v) {
		Node n = v.asNode();
		//TODO evaluate condition
		/*
        if ( ! n.isURI() )
            throw new ExprEvalException("Not a URI: " + FmtUtils.stringForNode(n)) ;
         */
		LOG.info(FmtUtils.stringForNode(n));
		return NodeValue.makeBoolean(true);
	}
}



