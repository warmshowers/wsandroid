package fi.bitrite.android.ws.search.impl;

public class SearchFailedException extends RuntimeException {

	private static final long serialVersionUID = 3260675894678151962L;

	public SearchFailedException(Exception e) {
		super(e);
	}

	public SearchFailedException(String msg) {
		super(msg);
	}

	public SearchFailedException() {
		super();
	}

}
