package fi.bitrite.android.ws.api;

import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;

public enum HttpSessionContainer {

    INSTANCE;

    private final BasicHttpContext httpContext;

    HttpSessionContainer() {
        httpContext = new BasicHttpContext();
        setCookieStore(new BasicCookieStore());
    }

    public BasicHttpContext getSessionContext() {
        return httpContext;
    }

    public void setCookieStore(BasicCookieStore cookieStore) {
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }
}
