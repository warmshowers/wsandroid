package fi.bitrite.android.ws.search.impl;

public class IncompleteResultsException extends HttpException {

	private static final long serialVersionUID = -5196011773520934900L;

	public IncompleteResultsException(Exception e) {
		super(e);
	}

	public IncompleteResultsException() {
		super();
	}

	public IncompleteResultsException(String msg) {
		super(msg);
	}

}
