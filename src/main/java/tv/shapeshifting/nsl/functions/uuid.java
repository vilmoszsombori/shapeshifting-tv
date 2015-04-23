package tv.shapeshifting.nsl.functions;

import java.util.UUID;

import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase0;

public class uuid extends FunctionBase0 {
	//private static Logger LOG = Logger.getLogger(stringEqual.class);

	public uuid() {
		super();
	}

	@Override
	public NodeValue exec() {
		return NodeValue.makeString(uuid.generate());
	}
	
	public static String generate() {
		UUID id = UUID.randomUUID();
		return "ID" + id.toString().replaceAll("-", "");
	}
	
	public static void main(String args[]) {
		for(int i = 0; i < 20 ; i++) {
			System.out.println("production:" + uuid.generate());
		}
	}
}