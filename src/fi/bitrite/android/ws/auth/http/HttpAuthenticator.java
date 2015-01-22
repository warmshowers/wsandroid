package fi.bitrite.android.ws.auth.http;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.http.HttpUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for authenticating the user against the WarmShowers web service.
 */
public class HttpAuthenticator {

    private final String wsUserAuthUrl = GlobalInfo.warmshowersBaseUrl + "/services/rest/user/login";
    private final String wsUserLogoutUrl = GlobalInfo.warmshowersBaseUrl + "/services/rest/user/logout";
    private final String wsUserAuthTestUrl = GlobalInfo.warmshowersBaseUrl + "/search/wsuser";

    private static final String TAG = "HttpAuthenticator";

    private List<NameValuePair> getCredentialsFromAccount() throws OperationCanceledException, AuthenticatorException, IOException, NoAccountException {
        List<NameValuePair> credentials = new ArrayList<NameValuePair>();

        String username = AuthenticationHelper.getAccountUsername();
        String password = AuthenticationHelper.getAccountPassword();

        credentials.add(new BasicNameValuePair("username", username));
        credentials.add(new BasicNameValuePair("password", password));
        return credentials;
    }

    /**
     * Returns
     * - userid
     * - 0 if already logged in
     */
    public int authenticate() throws HttpAuthenticationFailedException, IOException, JSONException, NoAccountException {
        RestClient authClient = new RestClient();

        try {
            authClient.post(wsUserLogoutUrl);
        } catch (Exception e) {
            Log.e(TAG, "Exception on logout: " + e.toString());
            // We don't care a lot about this, as we were just trying to ensure clean login.
        }

        HttpClient client = HttpUtils.getDefaultClient();
        HttpContext httpContext = HttpSessionContainer.INSTANCE.getSessionContext();
        int userId = 0;

        try {
            HttpPost post = new HttpPost(wsUserLogoutUrl);
            HttpResponse response = client.execute(post, httpContext);
        } catch (Exception e) {
            Log.e(TAG, "Exception on logout: " + e.toString());
            // We don't care a lot about this, as we were just trying to ensure clean login.
        }

        try {
            List<NameValuePair> credentials = getCredentialsFromAccount();

            HttpPost post = new HttpPost(wsUserAuthUrl);
            post.setEntity(new UrlEncodedFormEntity(credentials));
            HttpResponse response = client.execute(post, httpContext);

            HttpEntity entity = response.getEntity();
            String rawJson = EntityUtils.toString(entity, "UTF-8");

            if (rawJson.contains("Wrong username or password")) {
                throw new HttpAuthenticationFailedException("Wrong username or password");
            }

            JsonParser parser = new JsonParser();
            JsonObject o = (JsonObject) parser.parse(rawJson);
            String s = o.get("user").getAsJsonObject().get("uid").getAsString();
            userId = Integer.valueOf(s);

            String cookieSessionName = o.get("session_name").getAsString();
            String cookieSessionId = o.get("sessid").getAsString();

            AuthenticationHelper.addCookieInfo(cookieSessionName, cookieSessionId, userId);

        } catch (ClientProtocolException e) {
            if (e.getCause() instanceof CircularRedirectException) {
                // If we get this authentication has still been successful, so ignore it
            } else {
                throw new HttpAuthenticationFailedException(e);
            }
        } catch (IOException e) {
            // Rethrow, prevent the catch below from getting to it. we want to know this was IO exception
            throw e;
        } catch (Exception e) {
            // We might have had a json parsing or access exception - for example, if the "user" was not there,
            // Could also have AuthenticatorException or OperationCancelledException here
            // or if there was something wrong with what the server returned
            throw new HttpAuthenticationFailedException(e);
        } finally {
            client.getConnectionManager().shutdown();
        }

        return userId;
    }

}