package fi.bitrite.android.ws.api;

import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.auth.http.HttpAuthenticator;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.util.http.HttpException;
import fi.bitrite.android.ws.util.http.HttpUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import java.io.IOException;

/**
 * Base class for classes that use GET to either scrape the WS website for information
 * or interface with the REST API.
 */
public class HttpReader {

    boolean authenticationPerformed;

    public HttpReader() {
        setAuthenticationPerformed(false);
    }

    public boolean isAuthenticationPerformed() {
        return authenticationPerformed;
    }

    private void setAuthenticationPerformed(boolean authenticationPerformed) {
        this.authenticationPerformed = authenticationPerformed;
    }

    protected String getPage(String simpleUrl) throws HttpException {
        HttpClient client = HttpUtils.getDefaultClient();
        String html;
        int responseCode;

        try {
            String url = HttpUtils.encodeUrl(simpleUrl);
            HttpGet get = new HttpGet(url);
            HttpContext context = HttpSessionContainer.INSTANCE.getSessionContext();

            HttpResponse response = client.execute(get, context);
            HttpEntity entity = response.getEntity();
            responseCode = response.getStatusLine().getStatusCode();

            html = EntityUtils.toString(entity, "UTF-8");

            if (responseCode == HttpStatus.SC_FORBIDDEN ||
                    responseCode == HttpStatus.SC_UNAUTHORIZED) {
                if (!isAuthenticationPerformed()) {
                    authenticate();
                    html = getPage(simpleUrl);  // TODO: Remove ugly and unnecessary recursion
                } else {
                    throw new HttpException("Couldn't authenticate user");
                }
            }

        } catch (Exception e) {
            throw new HttpException(e);
        } finally {
            client.getConnectionManager().shutdown();
        }

        return html;
    }

    protected void authenticate() throws NoAccountException, IOException {
        HttpAuthenticator authenticator = new HttpAuthenticator();
        authenticator.authenticate();
        setAuthenticationPerformed(true);
    }

}
