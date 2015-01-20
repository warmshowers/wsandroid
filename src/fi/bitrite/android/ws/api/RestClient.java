package fi.bitrite.android.ws.api;

import android.content.Context;
import android.widget.Toast;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
import fi.bitrite.android.ws.auth.http.HttpAuthenticator;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.http.HttpException;
import fi.bitrite.android.ws.util.http.HttpUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.util.List;

/**
 * Base class for classes that use GET to either scrape the WS website for information
 * or interface with the REST API.
 */
public class RestClient {

    boolean authenticationPerformed;

    public RestClient() {
        setAuthenticationPerformed(false);
    }

    public boolean isAuthenticationPerformed() {
        return authenticationPerformed;
    }

    private void setAuthenticationPerformed(boolean authenticationPerformed) {
        this.authenticationPerformed = authenticationPerformed;
    }

    protected String get(String simpleUrl) throws HttpException {
        HttpClient client = HttpUtils.getDefaultClient();
        String json;
        int responseCode;

        try {
            String url = HttpUtils.encodeUrl(simpleUrl);
            HttpGet get = new HttpGet(url);
            HttpContext context = HttpSessionContainer.INSTANCE.getSessionContext();

            HttpResponse response = client.execute(get, context);
            HttpEntity entity = response.getEntity();
            responseCode = response.getStatusLine().getStatusCode();

            json = EntityUtils.toString(entity, "UTF-8");

            if (responseCode == HttpStatus.SC_FORBIDDEN ||
                    responseCode == HttpStatus.SC_UNAUTHORIZED) {
                if (!isAuthenticationPerformed()) {
                    authenticate();
                    json = get(simpleUrl);  // TODO: Remove ugly and unnecessary recursion
                } else {
                    throw new HttpException("Couldn't authenticate user");
                }
            }

        } catch (Exception e) {
            throw new HttpException(e);
        } finally {
            client.getConnectionManager().shutdown();
        }

        return json;
    }

    protected String post(String url, List<NameValuePair> params) throws HttpException, IOException {
        HttpClient client = HttpUtils.getDefaultClient();
        String json = "";

        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
            HttpContext httpContext = HttpSessionContainer.INSTANCE.getSessionContext();
            HttpResponse response = client.execute(post, httpContext);
            HttpEntity entity = response.getEntity();

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_FORBIDDEN ||
                    responseCode == HttpStatus.SC_UNAUTHORIZED) {
                if (!isAuthenticationPerformed()) {
                    authenticate();
                    return post(url, params);
                } else {
                    throw new HttpException("Couldn't authenticate user");
                }
            }

            json = EntityUtils.toString(entity, "UTF-8");

        }  finally {
            client.getConnectionManager().shutdown();
        }

        return json;
    }

    public static void reportError(Context context, Object obj) {
        if (obj instanceof Exception) {
            int rId = 0;
            if (obj instanceof NoAccountException) {
                rId = R.string.no_account;
            } else if (obj instanceof HttpAuthenticationFailedException) {
                rId = R.string.authentication_failed;
            } else if (obj instanceof HttpException) {
                rId = R.string.http_server_access_failure;
            } else if (obj instanceof IOException) {
                rId = R.string.io_error;
            } else {
                // Unexpected error
                rId = R.string.http_unexpected_failure;
            }
            Exception e = (Exception)obj;
            String exceptionDescription = e.toString();
            if (e.getMessage() != null) exceptionDescription += " Message:" + e.getMessage();
            if (e.getCause() != null) exceptionDescription += " Cause: " + e.getCause().toString();
            Tools.gaReportException(context, "RestClient Exception: ", exceptionDescription);

            Toast.makeText(context, rId, Toast.LENGTH_LONG).show();
        }
        return;
    }

    protected void authenticate() throws NoAccountException, IOException {
        HttpAuthenticator authenticator = new HttpAuthenticator();
        authenticator.authenticate();
        setAuthenticationPerformed(true);
    }

}
