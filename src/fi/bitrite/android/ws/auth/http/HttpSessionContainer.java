package fi.bitrite.android.ws.auth.http;

import org.apache.http.protocol.HttpContext;

public interface HttpSessionContainer {

	HttpContext getSessionContext();

}
