package fi.bitrite.android.ws.util.http;

public class HttpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

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
