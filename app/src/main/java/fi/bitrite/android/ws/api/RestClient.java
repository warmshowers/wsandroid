package fi.bitrite.android.ws.api;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.auth.AuthData;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.http.HttpException;
import fi.bitrite.android.ws.util.http.HttpUtils;

/**
 * Base class for classes that use GET to either scrape the WS website for information
 * or interface with the REST API.
 */
public class RestClient {

    public static final String TAG = "RestClient";

    private final AuthenticationController mAuthenticationController;

    public RestClient(AuthenticationController authenticationController) {
        mAuthenticationController = authenticationController;

        // Updates the cookie store on updated auth data.
        mAuthenticationController.getAuthData().subscribe(authData -> {
            BasicCookieStore cookieStore = new BasicCookieStore();

            if (authData != null && authData.authToken != null) {
                BasicClientCookie cookie = new BasicClientCookie(
                        authData.authToken.name, authData.authToken.id);
                cookie.setDomain(GlobalInfo.warmshowersCookieDomain);
                cookie.setPath("/");
                cookie.setSecure(true);

                cookieStore.addCookie(cookie);
            }

            HttpSessionContainer.INSTANCE.setCookieStore(cookieStore);
        });
    }

    public JSONObject get(String simpleUrl) throws HttpException, JSONException, URISyntaxException, IOException {
        HttpClient client = HttpUtils.getDefaultClient();
        String json;
        JSONObject jsonObj;
        int responseCode;

        String url = HttpUtils.encodeUrl(simpleUrl);
        HttpGet get = new HttpGet(url);

        HttpContext context = HttpSessionContainer.INSTANCE.getSessionContext();

        HttpResponse response = client.execute(get, context);
        HttpEntity entity = response.getEntity();
        responseCode = response.getStatusLine().getStatusCode();

        json = EntityUtils.toString(entity, "UTF-8");

        if (responseCode != HttpStatus.SC_OK) {
            Log.i(TAG, "Non-200 HTTP response(" + Integer.toString(responseCode) + " for URL " + url);
            handleErrorResponse(response);
            return get(url);
        }

        jsonObj = new JSONObject(json);
        client.getConnectionManager().shutdown();

        return jsonObj;
    }

    // Bare post with no params
    public JSONObject post(String url) throws HttpException, IOException, JSONException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        return post(url, params);
    }

    // Post with params
    public JSONObject post(String url, List<NameValuePair> params) throws HttpException, IOException, JSONException {
        HttpClient client = HttpUtils.getDefaultClient();
        String jsonString = "";
        JSONObject jsonObj;

        HttpPost httpPost = new HttpPost(url);

        AuthData authData = mAuthenticationController.getAuthData().getValue();
        if (authData != null && authData.csrfToken != null) {
            httpPost.addHeader("X-CSRF-Token", authData.csrfToken);
        }

        httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
        HttpContext httpContext = HttpSessionContainer.INSTANCE.getSessionContext();
        HttpResponse response = client.execute(httpPost, httpContext);
        HttpEntity entity = response.getEntity();

        int responseCode = response.getStatusLine().getStatusCode();

        if (responseCode != HttpStatus.SC_OK) {
            Log.i(TAG, "Non-200 HTTP response(" + Integer.toString(responseCode) + " for URL " + url);
            handleErrorResponse(response);
            return post(url, params);
        }

        jsonString = EntityUtils.toString(entity, "UTF-8");

        try {
            jsonObj = new JSONObject(jsonString);
        } catch (JSONException e) {  // Assume it might have been an array [true]
            JSONArray jsonArray = new JSONArray(jsonString);
            jsonObj = new JSONObject();
            jsonObj.put("arrayresult", jsonArray);
        }

        client.getConnectionManager().shutdown();


        return jsonObj;
    }

    private void handleErrorResponse(HttpResponse response) throws IOException, HttpException {
        int responseCode = response.getStatusLine().getStatusCode();
        switch (responseCode) {
            case HttpStatus.SC_UNAUTHORIZED:    // 401, typically when no CSRF token
                mAuthenticationController.getResponseInterceptorHandler().handleCsrfValidationError();
                break;

            case HttpStatus.SC_FORBIDDEN:       // 403 denied
                // If it is *not* an unauth for user 0/anonymous, it's for a properly auth user, so we have to bail.
                // Drupal 6 says "user 0", Drupal 7 says "user anonymous".
                // 401 for not logged in, Drupal7 gives 403 for no CSRF token or bad csrf token
                String statusLine = response.getStatusLine().getReasonPhrase();
                Pattern pattern = Pattern.compile("(denied for user (0|anonymous)|CSRF validation failed)");
                Matcher matcher = pattern.matcher(statusLine);
                if (!matcher.find()) {
                    throw new HttpException(Integer.toString(responseCode) + " error " + response.getStatusLine().getReasonPhrase());
                }

                boolean ok =
                        mAuthenticationController.getResponseInterceptorHandler().handleAuthTokenExpiration();
                if (!ok) {
                    throw new HttpException("Could not handle the authToken renewal.");
                }

                break;

            case HttpStatus.SC_NOT_ACCEPTABLE:  // 406, Typically trying to log out when not logged in
            default:
                throw new HttpException(Integer.toString(responseCode) + " " + response.getStatusLine().getReasonPhrase());
        }
    }


    public static void reportError(Context context, Object obj) {
        if (obj instanceof Exception) {
            int rId = 0;
            if (obj instanceof HttpException) {
                rId = R.string.http_server_access_failure;
            } else if (obj instanceof IOException) {
                rId = R.string.io_error;
            } else if (obj instanceof JSONException) {
                rId = R.string.json_error;
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
}
