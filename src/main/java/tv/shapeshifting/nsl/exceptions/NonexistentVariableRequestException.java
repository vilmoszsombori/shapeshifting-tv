package tv.shapeshifting.nsl.exceptions;

public class NonexistentVariableRequestException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5687103397412597812L;

	public NonexistentVariableRequestException() {
		super();
	}

	public NonexistentVariableRequestException(String message) {
		super(message);
	}	
}
