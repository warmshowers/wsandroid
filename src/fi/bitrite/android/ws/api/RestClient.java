package fi.bitrite.android.ws.api;

import android.accounts.AccountsException;
import android.content.Context;
import android.widget.Toast;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;

/**
 * Base class for classes that interface with the REST API via POST.
 * see https://github.com/rfay/Warmshowers.org/wiki/Warmshowers-RESTful-Services-for-Mobile-Apps
 */
public class RestClient extends HttpReader {

    private static final String TAG = "RestClient";

    protected String getJson(String url, List<NameValuePair> params) throws HttpException, IOException {
        HttpClient client = HttpUtils.getDefaultClient();
        String json = "";

        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(new UrlEncodedFormEntity(params));
            HttpContext httpContext = HttpSessionContainer.INSTANCE.getSessionContext();
            HttpResponse response = client.execute(post, httpContext);
            HttpEntity entity = response.getEntity();

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_FORBIDDEN ||
                    responseCode == HttpStatus.SC_UNAUTHORIZED) {
                if (!isAuthenticationPerformed()) {
                    authenticate();
                    return getJson(url, params);
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

    public static void reportError(Context context, Object e) {
        if (e instanceof Exception) {
            int rId = 0;
            if (e instanceof AccountsException) {
                rId = R.string.no_account;
            } else if (e instanceof HttpAuthenticationFailedException) {
                rId = R.string.authentication_failed;
            } else if (e instanceof HttpException) {
                rId = R.string.http_server_access_failure;
            } else if (e instanceof IOException) {
                rId = R.string.io_error;
            } else {
                // Unexpected error
                rId = R.string.http_unexpected_failure;
            }
            Tools.gaReportException(context, "Exception in RestClient: ", e.toString());

            Toast.makeText(context, rId, Toast.LENGTH_LONG).show();
        }
        return;
    }
}
