package fi.bitrite.android.ws.auth.http;

import fi.bitrite.android.ws.util.http.HttpException;

public class HttpAuthenticationFailedException extends HttpException {

    private static final long serialVersionUID = 1L;

    public HttpAuthenticationFailedException(Exception e) {
        super(e);
    }

    public HttpAuthenticationFailedException(String msg) {
        super(msg);
    }
}
