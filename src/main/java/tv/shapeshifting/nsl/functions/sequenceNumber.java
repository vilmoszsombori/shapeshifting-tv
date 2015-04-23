package tv.shapeshifting.nsl.functions;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase1;

public class sequenceNumber extends FunctionBase1 {
	private static Logger LOG = Logger.getLogger(sequenceNumber.class);
	private int count = 0;

	public sequenceNumber() {
		super();
		LOG.debug("sequenceNumber initiated. count = " + count + ".");
	}

	@Override
	public NodeValue exec(NodeValue v) {		
		String s = v.asUnquotedString();
		int aim = Integer.parseInt(s);
		LOG.debug("count = " + count + ", aim = " + aim);
		NodeValue ret = NodeValue.makeBoolean( count == aim );
		count++;
		return ret;
	}
}
