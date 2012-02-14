package fi.bitrite.android.ws.auth.http;

public class HttpAuthenticationFailedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public HttpAuthenticationFailedException(Exception e) {
		super(e);
	}

	public HttpAuthenticationFailedException(String msg) {
		super(msg);
	}
}
