/***
 Expression parser
 Reuse code from: http://www.speqmath.com/tutorials/expression_parser_java/index.html
 
 Features:
 Operators:
 & | << >>
 = <> < > <= >=
 + -
 * / % ||
 ^
 !

 Functions:
 Abs, Exp, Sign, Sqrt, Log, Log10
 Sin, Cos, Tan, ASin, ACos, ATan
 Factorial

 Variables:
 Pi, e
 you can define your own variables

 Other:
 Scientific notation supported
 Error handling supported

 */

package tv.shapeshifting.nsl;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import tv.shapeshifting.nsl.exceptions.ArithmeticExpressionEvaluationException;
import tv.shapeshifting.nsl.util.Maths;

public class ArithmeticExpressionParser {
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		ArithmeticExpressionParser prs = new ArithmeticExpressionParser();
		String expr;

		System.out
				.println("Enter an expression an press Enter to calculate the result.");
		System.out.println("Enter an empty expression to quit.");
		System.out.println("");

		do {
			// request an expression
			System.out.print("> ");
			expr = in.nextLine();

			if (expr.length() > 0) {
				// evaluate the expression
				String ans_str;
				try {					
					double ans = prs.parse(expr);						
					ans_str = String.format("Ans = %g", ans);
				} catch (ArithmeticExpressionEvaluationException err) {
					ans_str = err.get();
				}								
				System.out.println("\t" + ans_str);
			}
		} while (expr.length() > 0);
		in.close();
	}

	/**
	 * finalructor. Initializes all data with zeros and empty strings
	 */
	public ArithmeticExpressionParser() {
		expr = "";
		expr_pos = -1;
		expr_c = '\0';

		token = "";
		token_type = TOKENTYPE.NOTHING;
	}

	/**
	 * parses and evaluates the given expression On error, an exception of type
	 * ArithmeticExpressionEvaluationException is thrown
	 * @throws ArithmeticExpressionEvaluationException 
	 */
	public double parse(final String new_expr) throws ArithmeticExpressionEvaluationException {
		// initialize all variables
		expr = new_expr; // copy the given expression to expr
		ans = 0.0;
		var_dependencies.clear(); // clear the variable dependency set

		// get the first character in expr
		getFirstChar();

		getToken();

		// check whether the given expression is empty
		if (token_type == TOKENTYPE.DELIMETER && expr_c == '\0') {
			throw new ArithmeticExpressionEvaluationException(row(), col(), 4);
		}

		ans = parse_level1();
		
		// check for garbage at the end of the expression
		if (token_type != TOKENTYPE.DELIMETER || token.length() > 0) {
			if (token_type == TOKENTYPE.DELIMETER) {
				// user entered a not existing operator like "//"
				throw new ArithmeticExpressionEvaluationException(row(), col(), 101, token);
			} else {
				throw new ArithmeticExpressionEvaluationException(row(), col(), 5, token);
			}
		}
		
		return ans;
	}
	
	
	/**
	 * Updates the user variable table
	 * @param user_var
	 */
	public void updateUserVariables(Map<String, Double> user_var) {
		this.user_var.clear();
		this.user_var.putAll(user_var);
		//this.user_var = user_var;		
	}
	
	/**
	 * Sets the user variable table
	 * @param user_var
	 */
	public void setUserVariables(Map<String, Double> user_var) {
		this.user_var = user_var;		
	}
	
	/**
	 * Returns the variable table
	 * @return Map<String, Double>
	 */
	public final Map<String, Double> getUserVariables() {
		return this.user_var;
	}
	
	/**
	 * Returns the user variable dependency set
	 * @return Set<String>
	 */
	public final Set<String> getUserVariableDependencies() {
		return var_dependencies;		
	}

	/**
	 * checks if the given char c is a minus
	 */
	boolean isMinus(final char c) {
		return c == '-';
	}

	/**
	 * checks if the given char c is whitespace whitespace when space chr(32) or
	 * tab chr(9)
	 */
	boolean isWhiteSpace(final char c) {
		return c == 32 || c == 9; // space or tab
	}

	/**
	 * checks if the given char c is a delimeter minus is checked apart, can be
	 * unary minus
	 */
	boolean isDelimeter(final char c) {
		return "&|<>=+/*%^!".indexOf(c) != -1;
	}

	/**
	 * checks if the given char c is NO delimeter
	 */
	boolean isNotDelimeter(final char c) {
		return "&|<>=+-/*%^!()".indexOf(c) != -1;
	}

	/**
	 * checks if the given char c is a letter or undersquare
	 */
	boolean isAlpha(final char c) {
		char cUpper = Character.toUpperCase(c);
		return "ABCDEFGHIJKLMNOPQRSTUVWXYZ_".indexOf(cUpper) != -1;
	}

	/**
	 * checks if the given char c is a digit or dot
	 */
	boolean isDigitDot(final char c) {
		return "0123456789.".indexOf(c) != -1;
	}

	/**
	 * checks if the given char c is a digit
	 */
	boolean isDigit(final char c) {
		return "0123456789".indexOf(c) != -1;
	}

	/**
	 * TODO check for reserved variable names
	 */
	boolean isLegalVariableName(String name) {
		return true;
		/* checks if the given variable name is legal to use, i.e. not equal to
		 * "pi", "e", etc.
		 * 
		String nameUpper = name.toUpperCase();
		if (nameUpper.equals("E"))
			return false;
		if (nameUpper.equals("PI"))
			return false;

		return true;
		 */	
	}
	
	/**
	 * checks if the given char c is a quote mark
	 */
	boolean isQuote(final char c) {
		return "'\"".indexOf(c) != -1;		
	}

	/**
	 * Get the next character from the expression. The character is stored into
	 * the char expr_c. If the end of the expression is reached, the function
	 * puts zero ('\0') in expr_c.
	 */
	void getChar() {
		expr_pos++;
		if (expr_pos < expr.length()) {
			expr_c = expr.charAt(expr_pos);
		} else {
			expr_c = '\0';
		}
	}

	/**
	 * Get the first character from the expression. The character is stored into
	 * the char expr_c. If the end of the expression is reached, the function
	 * puts zero ('\0') in expr_c.
	 */
	void getFirstChar() {
		expr_pos = 0;
		if (expr_pos < expr.length()) {
			expr_c = expr.charAt(expr_pos);
		} else {
			expr_c = '\0';
		}
	}

	/***
	 * Get next token in the current string expr. Uses the Parser data expr, e,
	 * token, t, token_type and err
	 */
	void getToken() throws ArithmeticExpressionEvaluationException {
		token_type = TOKENTYPE.NOTHING;
		token = ""; // set token empty

		// skip over whitespaces
		while (isWhiteSpace(expr_c)) // space or tab
		{
			getChar();
		}

		// check for end of expression
		if (expr_c == '\0') {
			// token is empty
			token_type = TOKENTYPE.DELIMETER;
			return;
		}

		// check for minus
		if (expr_c == '-') {
			token_type = TOKENTYPE.DELIMETER;
			token += expr_c;
			getChar();
			return;
		}

		// check for parentheses
		if (expr_c == '(' || expr_c == ')') {
			token_type = TOKENTYPE.DELIMETER;
			token += expr_c;
			getChar();
			return;
		}

		// check for operators (delimeters)
		if (isDelimeter(expr_c)) {
			token_type = TOKENTYPE.DELIMETER;
			while (isDelimeter(expr_c)) {
				token += expr_c;
				getChar();
			}
			return;
		}

		// check for a value
		if (isDigitDot(expr_c)) {
			token_type = TOKENTYPE.NUMBER;
			while (isDigitDot(expr_c)) {
				token += expr_c;
				getChar();
			}

			// check for scientific notation like "2.3e-4" or "1.23e50"
			if (expr_c == 'e' || expr_c == 'E') {
				token += expr_c;
				getChar();

				if (expr_c == '+' || expr_c == '-') {
					token += expr_c;
					getChar();
				}

				while (isDigit(expr_c)) {
					token += expr_c;
					getChar();
				}
			}

			return;
		}

		// check for variables or functions
		if (isAlpha(expr_c) || isQuote(expr_c)) {
			
			// keep whitespaces in between quotes (quoted variable names)
			if (isQuote(expr_c)) {
				/* Skip over the starting quote mark
				 * token += expr_c;
				 */
				getChar();
				
				while (!isQuote(expr_c)) {
					token += expr_c;
					getChar();
				}
				
				if (isQuote(expr_c)) {
					/* Skip over the ending quote mark
					 * token += expr_c;
					 */
					getChar();					
				}
			} else {
			
				while (isAlpha(expr_c) || isDigit(expr_c)) {
					token += expr_c;
					getChar();
				}
			}

			// skip whitespaces
			while (isWhiteSpace(expr_c)) // space or tab
			{
				getChar();
			}

			// check the next non-whitespace character
			if (expr_c == '(') {
				token_type = TOKENTYPE.FUNCTION;
			} else {
				token_type = TOKENTYPE.VARIABLE;
			}

			return;
		}

		// something unknown is found, wrong characters -> a syntax error
		token_type = TOKENTYPE.UNKNOWN;
		while (expr_c != '\0') {
			token += expr_c;
			getChar();
		}

		throw new ArithmeticExpressionEvaluationException(row(), col(), 1, token);
	}

	/**
	 * assignment of variable or function
	 */
	double parse_level1() throws ArithmeticExpressionEvaluationException {
		if (token_type == TOKENTYPE.VARIABLE) {
			// skip whitespaces
			while (isWhiteSpace(expr_c)) // space or tab
			{
				getChar();
			}
			
			/* do a 2-lookahead to see if the next token 
			 * is an equal operator ("==") or assignment ("=") 
			 */
			char next_expr_c = '\0';
			if (expr_pos + 1 < expr.length()) {
				next_expr_c = expr.charAt(expr_pos + 1);
			}			

			// check the next non-whitespace character
			if (expr_c == '=' && next_expr_c != '=') {
				String var_name = token;

				// get the token '='
				getToken();
				
				// assignment
				double ans;
				getToken();
				ans = parse_level2();
				
				// check whether the token is a legal name
				if (isLegalVariableName(var_name)) {
					user_var.put(var_name, new Double(ans));
				} else {
					throw new ArithmeticExpressionEvaluationException(row(), col(), 300);
				}
				return ans;
			}
		}

		return parse_level2();
	}

	/**
	 * conditional operators and bitshift
	 */
	double parse_level2() throws ArithmeticExpressionEvaluationException {
		OPERATOR op_id;
		double ans;
		ans = parse_level3();

		op_id = get_operator_id(token);
		while (op_id == OPERATOR.AND || op_id == OPERATOR.OR
				|| op_id == OPERATOR.BITSHIFTLEFT
				|| op_id == OPERATOR.BITSHIFTRIGHT) {
			getToken();
			ans = eval_operator(op_id, ans, parse_level3());
			op_id = get_operator_id(token);
		}

		return ans;
	}

	/**
	 * conditional operators
	 */
	double parse_level3() throws ArithmeticExpressionEvaluationException {
		OPERATOR op_id;
		double ans;
		ans = parse_level4();

		op_id = get_operator_id(token);
		while (op_id == OPERATOR.EQUAL || op_id == OPERATOR.UNEQUAL
				|| op_id == OPERATOR.SMALLER || op_id == OPERATOR.LARGER
				|| op_id == OPERATOR.SMALLEREQ || op_id == OPERATOR.LARGEREQ) {
			getToken();
			ans = eval_operator(op_id, ans, parse_level4());
			op_id = get_operator_id(token);
		}

		return ans;
	}

	/**
	 * add or subtract
	 */
	double parse_level4() throws ArithmeticExpressionEvaluationException {
		OPERATOR op_id;
		double ans;
		ans = parse_level5();

		op_id = get_operator_id(token);
		while (op_id == OPERATOR.PLUS || op_id == OPERATOR.MINUS) {
			getToken();
			ans = eval_operator(op_id, ans, parse_level5());
			op_id = get_operator_id(token);
		}

		return ans;
	}

	/**
	 * multiply, divide, modulus, xor
	 */
	double parse_level5() throws ArithmeticExpressionEvaluationException {
		OPERATOR op_id;
		double ans;
		ans = parse_level6();

		op_id = get_operator_id(token);
		while (op_id == OPERATOR.MULTIPLY || op_id == OPERATOR.DIVIDE
				|| op_id == OPERATOR.MODULUS || op_id == OPERATOR.XOR) {
			getToken();
			ans = eval_operator(op_id, ans, parse_level6());
			op_id = get_operator_id(token);
		}

		return ans;
	}

	/**
	 * power
	 */
	double parse_level6() throws ArithmeticExpressionEvaluationException {
		OPERATOR op_id;
		double ans;
		ans = parse_level7();

		op_id = get_operator_id(token);
		while (op_id == OPERATOR.POW) {
			getToken();
			ans = eval_operator(op_id, ans, parse_level7());
			op_id = get_operator_id(token);
		}

		return ans;
	}

	/**
	 * Factorial
	 * TODO implement "logical not" instead of factorial 
	 */
	double parse_level7() throws ArithmeticExpressionEvaluationException {
		OPERATOR op_id;
		double ans;
		ans = parse_level8();

		op_id = get_operator_id(token);
		while (op_id == OPERATOR.FACTORIAL) {
			getToken();
			// factorial does not need a value right from the
			// operator, so zero is filled in.
			ans = eval_operator(op_id, ans, 0.0);
			op_id = get_operator_id(token);
		}

		return ans;
	}

	/**
	 * Unary minus
	 */
	double parse_level8() throws ArithmeticExpressionEvaluationException {
		double ans;

		OPERATOR op_id = get_operator_id(token);
		if (op_id == OPERATOR.MINUS) {
			getToken();
			ans = parse_level9();
			ans = -ans;
		} else {
			ans = parse_level9();
		}

		return ans;
	}

	/**
	 * functions
	 */
	double parse_level9() throws ArithmeticExpressionEvaluationException {
		String fn_name;
		double ans;

		if (token_type == TOKENTYPE.FUNCTION) {
			fn_name = token;
			getToken();
			ans = eval_function(fn_name, parse_level10());
		} else {
			ans = parse_level10();
		}

		return ans;
	}

	/**
	 * parenthesized expression or value
	 */
	double parse_level10() throws ArithmeticExpressionEvaluationException {
		// check if it is a parenthesized expression
		if (token_type == TOKENTYPE.DELIMETER) {
			if (token.equals("(")) {
				getToken();
				double ans = parse_level2();
				if (token_type != TOKENTYPE.DELIMETER || !token.equals(")")) {
					throw new ArithmeticExpressionEvaluationException(row(), col(), 3);
				}
				getToken();
				return ans;
			}
		}

		// if not parenthesized then the expression is a value
		return parse_number();
	}

	double parse_number() throws ArithmeticExpressionEvaluationException {
		double ans = 0.0;

		switch (token_type) {
		case NUMBER:
			// this is a number
			ans = Double.parseDouble(token);
			getToken();
			break;

		case VARIABLE:
			// this is a variable
			ans = eval_variable(token);
			getToken();
			break;

		default:
			// syntax error or unexpected end of expression
			if (token.length() == 0) {
				throw new ArithmeticExpressionEvaluationException(row(), col(), 6);
			} else {
				throw new ArithmeticExpressionEvaluationException(row(), col(), 7);
			}
		}

		return ans;
	}

	/**
	 * returns the id of the given operator treturns -1 if the operator is not
	 * recognized
	 */
	OPERATOR get_operator_id(final String op_name) {
		// level 2
		if (op_name.equals("&")) {
			return OPERATOR.AND;
		}
		if (op_name.equals("|")) {
			return OPERATOR.OR;
		}
		if (op_name.equals("<<")) {
			return OPERATOR.BITSHIFTLEFT;
		}
		if (op_name.equals(">>")) {
			return OPERATOR.BITSHIFTRIGHT;
		}

		// level 3
		if (op_name.equals("==")) { 
			return OPERATOR.EQUAL;
		}
		if (op_name.equals("<>")) { //TODO check operator
			return OPERATOR.UNEQUAL;
		}
		if (op_name.equals("!=")) {
			return OPERATOR.UNEQUAL;
		}
		if (op_name.equals("<")) {
			return OPERATOR.SMALLER;
		}
		if (op_name.equals(">")) {
			return OPERATOR.LARGER;
		}
		if (op_name.equals("<=")) {
			return OPERATOR.SMALLEREQ;
		}
		if (op_name.equals(">=")) {
			return OPERATOR.LARGEREQ;
		}

		// level 4
		if (op_name.equals("+")) {
			return OPERATOR.PLUS;
		}
		if (op_name.equals("-")) {
			return OPERATOR.MINUS;
		}

		// level 5
		if (op_name.equals("*")) {
			return OPERATOR.MULTIPLY;
		}
		if (op_name.equals("/")) {
			return OPERATOR.DIVIDE;
		}
		if (op_name.equals("%")) {
			return OPERATOR.MODULUS;
		}
		if (op_name.equals("||")) {
			return OPERATOR.XOR;
		}

		// level 6
		if (op_name.equals("^")) {
			return OPERATOR.POW;
		}

		// level 7
		if (op_name.equals("!")) {
			return OPERATOR.FACTORIAL;
		}

		return OPERATOR.UNKNOWN;
	}

	/**
	 * evaluate an operator for given valuess
	 */
	double eval_operator(final OPERATOR op_id, final double lhs,
			final double rhs) throws ArithmeticExpressionEvaluationException {
		switch (op_id) {
		// level 2
		case AND:
			return (int) lhs & (int) rhs;
		case OR:
			return (int) lhs | (int) rhs;
		case BITSHIFTLEFT:
			return (int) lhs << (int) rhs;
		case BITSHIFTRIGHT:
			return (int) lhs >> (int) rhs;

			// level 3
		case EQUAL:
			return (lhs == rhs) ? 1.0 : 0.0;
		case UNEQUAL:
			return (lhs != rhs) ? 1.0 : 0.0;
		case SMALLER:
			return (lhs < rhs) ? 1.0 : 0.0;
		case LARGER:
			return (lhs > rhs) ? 1.0 : 0.0;
		case SMALLEREQ:
			return (lhs <= rhs) ? 1.0 : 0.0;
		case LARGEREQ:
			return (lhs >= rhs) ? 1.0 : 0.0;

			// level 4
		case PLUS:
			return lhs + rhs;
		case MINUS:
			return lhs - rhs;

			// level 5
		case MULTIPLY:
			return lhs * rhs;
		case DIVIDE:
			return lhs / rhs;
		case MODULUS:
			return Maths.modulus(lhs, rhs);
		case XOR:
			return (int) lhs ^ (int) rhs;

			// level 6
		case POW:
			return Math.pow(lhs, rhs);

			// level 7
		case FACTORIAL:
			return Maths.factorial(lhs);
		default:
			break;
		}

		throw new ArithmeticExpressionEvaluationException(row(), col(), 104);
	}

	/**
	 * evaluate a function
	 */
	double eval_function(final String fn_name, final double value) throws ArithmeticExpressionEvaluationException {
		// first make the function name upper case
		String fnUpper = fn_name.toUpperCase();

		// arithmetic
		if (fnUpper.equals("ABS")) {
			return Math.abs(value);
		}
		if (fnUpper.equals("EXP")) {
			return Math.exp(value);
		}
		if (fnUpper.equals("SIGN")) {
			return Maths.sign(value);
		}
		if (fnUpper.equals("SQRT")) {
			return Math.sqrt(value);
		}
		if (fnUpper.equals("LOG")) {
			return Math.log(value);
		}
		if (fnUpper.equals("LOG10")) {
			return Math.log10(value);
		}

		// trigonometric
		if (fnUpper.equals("SIN")) {
			return Math.sin(value);
		}
		if (fnUpper.equals("COS")) {
			return Math.cos(value);
		}
		if (fnUpper.equals("TAN")) {
			return Math.tan(value);
		}
		if (fnUpper.equals("ASIN")) {
			return Math.asin(value);
		}
		if (fnUpper.equals("ACOS")) {
			return Math.acos(value);
		}
		if (fnUpper.equals("ATAN")) {
			return Math.atan(value);
		}

		// probability
		if (fnUpper.equals("FACTORIAL")) {
			return Maths.factorial(value);
		}

		// unknown function
		throw new ArithmeticExpressionEvaluationException(row(), col(), 102, fn_name);
	}

	/**
	 * evaluate a variable
	 */
	double eval_variable(final String var_name) throws ArithmeticExpressionEvaluationException {
		/* TODO check if there are other parts in this class where the assumption is 
		 * that the variable names are upper case only
		 */
		
		/*
		// first make the variable name uppercase
		String varUpper = var_name.toUpperCase(); 

		// check for built-in variables
		if (varUpper.equals("E")) {
			return Math.E;
		}
		if (varUpper.equals("PI")) {
			return Math.PI;
		}

		// check for user defined variables
		if (user_var.containsKey(varUpper)) {
			double ans = user_var.get(varUpper).doubleValue();
			return ans;
		}
		*/

		// check for user defined variables
		if (user_var.containsKey(var_name)) {
			double ans = user_var.get(var_name).doubleValue();
			var_dependencies.add(var_name);
			return ans;
		}
				
		// unknown variable
		throw new ArithmeticExpressionEvaluationException(row(), col(), 103, var_name);
	}

	/**
	 * Shortcut for getting the current row value (one based) Returns the line
	 * of the currently handled expression
	 */
	int row() {
		return -1;
	}

	/**
	 * Shortcut for getting the current col value (one based) Returns the column
	 * (position) where the last token starts
	 */
	int col() {
		return expr_pos - token.length() + 1;
	}

	// / private enumerations
	private enum TOKENTYPE {
		NOTHING, DELIMETER, NUMBER, VARIABLE, FUNCTION, UNKNOWN
	}

	private enum OPERATOR {
		UNKNOWN, AND, OR, BITSHIFTLEFT, BITSHIFTRIGHT, // level 2
		EQUAL, UNEQUAL, SMALLER, LARGER, SMALLEREQ, LARGEREQ, // level 3
		PLUS, MINUS, // level 4
		MULTIPLY, DIVIDE, MODULUS, XOR, // level 5
		POW, // level 6
		FACTORIAL
	} // level 7

	// / private data
	private String expr; // / holds the expression
	private int expr_pos; // / points to the current position in expr
	private char expr_c; // / holds the current character from expr

	private String token; // / holds the token
	private TOKENTYPE token_type; // / type of the token

	private double ans; // / holds the result of the expression

	// / list with variables defined by user
	private Map<String, Double> user_var = new HashMap<String, Double>();
	private Set<String> var_dependencies = new HashSet<String>();
}
