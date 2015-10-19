package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.util.http.HttpException;

public class IncompleteResultsException extends HttpException {

    private static final long serialVersionUID = 1L;

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
