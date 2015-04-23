package tv.shapeshifting.nsl.functions;

import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase2;

public class stringEqual extends FunctionBase2 {
	//private static Logger LOG = Logger.getLogger(stringEqual.class);

	public stringEqual() {
		super();
	}

	@Override
	public NodeValue exec(NodeValue v1, NodeValue v2) {
		String s1 = v1.asString();
		String s2 = v2.asString();
		//LOG.info("stringEqual(" + s1 + ", " + s2 + ")");
		if(s1.equals(s2))
			return NodeValue.makeBoolean(true);
		else
			return NodeValue.makeBoolean(false);
	}
}

