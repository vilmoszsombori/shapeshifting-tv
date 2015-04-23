package tv.shapeshifting.nsl.functions;

import java.util.Random;

import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase1;

import org.apache.log4j.Logger;

public class random extends FunctionBase1 {
	private static Logger LOG = Logger.getLogger(random.class);

	public random() {
		super();
	}

	@Override
	public NodeValue exec(NodeValue v) {
		String s = v.asUnquotedString();
		int count = Integer.parseInt(s);
		Random random = new Random();
		int r = random.nextInt(count);
		LOG.debug("Random [" + r + "] returned from [" + count + "].");
		return NodeValue.makeInteger(r);
	}
}

