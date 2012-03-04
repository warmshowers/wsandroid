package fi.bitrite.android.ws.search.impl;

public class TooManyHostsException extends HttpException {

	private int numHosts;
	
	private static final long serialVersionUID = 8194674087269749871L;

	public TooManyHostsException(int numHosts) {
		this.numHosts = numHosts;
	}

	public int getNumHosts() {
		return numHosts;
	}

}
