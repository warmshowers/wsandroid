package fi.bitrite.android.ws.auth.http;

import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;

public enum HttpSessionContainer {

    INSTANCE;

    private final BasicCookieStore cookieStore;
    private final BasicHttpContext httpContext;

    HttpSessionContainer() {
        cookieStore = new BasicCookieStore();
        httpContext = new BasicHttpContext();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public BasicHttpContext getSessionContext() {
        return httpContext;
    }
}
