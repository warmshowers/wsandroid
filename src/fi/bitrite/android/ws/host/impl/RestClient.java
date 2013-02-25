package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.util.http.HttpUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.util.List;

/**
 * Base class for classes that interface with the REST API via POST.
 * @see https://github.com/rfay/Warmshowers.org/wiki/Warmshowers-RESTful-Services-for-Mobile-Apps
 */
public class RestClient extends HttpReader {

    public RestClient(HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer) {
        super(authenticationService, sessionContainer);
    }

    protected String getJson(String url, List<NameValuePair> params) {
        HttpClient client = HttpUtils.getDefaultClient();
        String json;

		try {
            HttpPost post = new HttpPost(url);
            post.setEntity(new UrlEncodedFormEntity(params));
            HttpContext httpContext = sessionContainer.getSessionContext();
            HttpResponse response = client.execute(post, httpContext);

			HttpEntity entity = response.getEntity();
            json = EntityUtils.toString(entity, "UTF-8");
		}

		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}

		return json;
    }
}
