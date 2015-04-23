package tv.shapeshifting.nsl.util;

import tv.shapeshifting.nsl.exceptions.ArithmeticExpressionEvaluationException;

/**
 * Additional functions
 */

public class Maths {
	/**
	 * calculate factorial of value for example 5! = 5*4*3*2*1 = 120
	 */
	public static double factorial(double value) throws ArithmeticExpressionEvaluationException {
		double res;
		int v = (int) value;

		if (value != v) {
			throw new ArithmeticExpressionEvaluationException(400, "factorial");
		}

		res = v;
		v--;
		while (v > 1) {
			res *= v;
			v--;
		}

		if (res == 0)
			res = 1; // 0! is per definition 1
		return res;
	}

	/**
	 * calculate the modulus of the given values
	 */
	public static double modulus(double a, double b) throws ArithmeticExpressionEvaluationException {
		// values must be integer
		int a_int = (int) a;
		int b_int = (int) b;
		if (a_int == a && b_int == b) {
			return a_int % b_int;
		} else {
			throw new ArithmeticExpressionEvaluationException(400, "%");
		}
	}

	/**
	 * calculate the sign of the given value
	 */
	public static double sign(double value) {
		if (value > 0)
			return 1;
		if (value < 0)
			return -1;
		return 0;
	}
}
