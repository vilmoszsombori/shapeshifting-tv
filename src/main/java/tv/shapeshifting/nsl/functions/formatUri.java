package tv.shapeshifting.nsl.functions;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase1;
import com.hp.hpl.jena.sparql.util.FmtUtils;

public class formatUri extends FunctionBase1 {
	private static Logger LOG = Logger.getLogger(formatUri.class);

	public formatUri() {
		super();
	}

	@Override
	public NodeValue exec(NodeValue v) {
		Node n = v.asNode();

		String s;

		if ( ! n.isURI() ) {
			s = FmtUtils.stringForNode(n);
            LOG.warn("Not a URI: " + FmtUtils.stringForNode(n)) ;
        } else {
        	s = n.getURI();
        }
                
        return NodeValue.makeString(formatUri.from(s));
	}
	
	public static String from(String uri) {
		String s = uri;
        if ( s.startsWith("http://") ) {
        	s = s.substring(s.lastIndexOf('/') + 1);
        	s = s.replace('#', ':');
        }
        return s;	
	}
}



