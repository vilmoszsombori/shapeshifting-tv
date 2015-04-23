package tv.shapeshifting.nsl.util;

public class StringFunction {

	public static String toVariableName(String in) {
		return in.replaceAll("[^\\p{L}\\p{N}]", "");
	}
}
