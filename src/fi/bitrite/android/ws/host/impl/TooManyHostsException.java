package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.util.http.HttpException;

public class TooManyHostsException extends HttpException {

    private static final long serialVersionUID = 1L;

    private final int numHosts;
    
    public TooManyHostsException(int numHosts) {
        this.numHosts = numHosts;
    }

    public int getNumHosts() {
        return numHosts;
    }

}
