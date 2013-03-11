package fi.bitrite.android.ws.api;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.util.http.HttpException;
import fi.bitrite.android.ws.util.http.HttpUtils;

/**
 * Base class for classes that use GET to either scrape the WS website for information
 * or interface with the REST API.
 */
public class HttpReader {

    protected HttpAuthenticationService authenticationService;
    protected HttpSessionContainer sessionContainer;
    protected boolean authenticationPerformed;

    public HttpReader(HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer) {
        this.authenticationService = authenticationService;
        this.sessionContainer = sessionContainer;
        setAuthenticationPerformed(false);
    }

    public boolean isAuthenticationPerformed() {
        return authenticationPerformed;
    }

    private void setAuthenticationPerformed(boolean authenticationPerformed) {
        this.authenticationPerformed = authenticationPerformed;
    }

    protected String getPage(String simpleUrl) {
        HttpClient client = HttpUtils.getDefaultClient();
        String html;
        int responseCode;

        try {
            String url = HttpUtils.encodeUrl(simpleUrl);
            HttpGet get = new HttpGet(url);
            HttpContext context = sessionContainer.getSessionContext();

            HttpResponse response = client.execute(get, context);
            HttpEntity entity = response.getEntity();
            responseCode = response.getStatusLine().getStatusCode();

            html = EntityUtils.toString(entity, "UTF-8");
        } 
        
        catch (Exception e) {
            throw new HttpException(e);
        }

        finally {
            client.getConnectionManager().shutdown();
        }

        if (responseCode == HttpStatus.SC_FORBIDDEN) {
            if (!authenticationPerformed) {
                authenticate();
                html = getPage(simpleUrl);
            } else {
                throw new HttpException("Couldn't authenticate user");
            }
        }

        return html;
    }

    protected void authenticate() {
        authenticationService.authenticate();
        setAuthenticationPerformed(true);
    }

}
