package tv.shapeshifting.nsl.exceptions;

public class UnrecognizedMediatypeException extends Exception {

	private static final long serialVersionUID = 5025318598924237989L;

	public UnrecognizedMediatypeException() {
		super();
	}

	public UnrecognizedMediatypeException(String message) {
		super(message);
	}
}
