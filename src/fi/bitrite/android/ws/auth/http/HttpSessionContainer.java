package fi.bitrite.android.ws.auth.http;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Singleton;

@Singleton
public class HttpSessionContainer {

	CookieStore cookieStore;

	HttpContext httpContext;
	
	public HttpSessionContainer() {
		cookieStore = new BasicCookieStore();
		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	}

	public HttpContext getSessionContext() {
		return httpContext;
	}
	
}
