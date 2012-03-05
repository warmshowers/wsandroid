package fi.bitrite.android.ws.util.http;

public class HttpException extends RuntimeException {

	private static final long serialVersionUID = 3260675894678151962L;

	public HttpException(Exception e) {
		super(e);
	}

	public HttpException(String msg) {
		super(msg);
	}

	public HttpException() {
		super();
	}

}
