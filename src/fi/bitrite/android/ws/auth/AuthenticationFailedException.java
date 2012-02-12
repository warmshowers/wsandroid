package fi.bitrite.android.ws.auth;

public class AuthenticationFailedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AuthenticationFailedException(Exception e) {
		super(e);
	}

	public AuthenticationFailedException(String msg) {
		super(msg);
	}
}
