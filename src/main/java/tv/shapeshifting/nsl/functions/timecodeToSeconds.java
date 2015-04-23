package tv.shapeshifting.nsl.functions;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.exceptions.TimecodeFormatException;
import tv.shapeshifting.nsl.util.Timecode;

import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase1;

public class timecodeToSeconds extends FunctionBase1 {
	private static Logger LOG = Logger.getLogger(timecodeToSeconds.class);

	public timecodeToSeconds() {
		super();
	}

	@Override
	public NodeValue exec(NodeValue v) {
		try {
			//LOG.info(v.asString());
			return NodeValue.makeString(Timecode.parse(v.asString()).toSeconds());
		} catch (TimecodeFormatException e) {
            LOG.warn("TimecodeFormatException: " + v.getString()) ;			
			e.printStackTrace();
			return NodeValue.makeString("picsa");
		}
	}	
}


