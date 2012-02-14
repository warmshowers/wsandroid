package fi.bitrite.android.ws.auth.http;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import fi.bitrite.android.ws.auth.CredentialsService;

/**
 * Responsible for authenticating the user against the WarmShowers web service.
 * Authentication is done using a session cookie which is stored here in the service.
 */
@Singleton
public class HttpAuthenticationServiceProvider implements HttpAuthenticationService {

	private static final String WARMSHOWERS_USER_AUTHENTICATION_URL = "http://www.warmshowers.org/user";

	@Inject
	CredentialsService credentialsService;
	
	@Inject
	HttpSessionContainer sessionContainer;
	
	boolean authenticated = false;

	public boolean isAuthenticated() {
		// todo: do a HTTP GET to see if we are authenticated?
		return authenticated;
	}

	public void authenticate() {
		// Log.d("authenticate", "Username: " + credentialsService.getUsername() + ", Password: " + credentialsService.getPassword());
		
		authenticated = false;

		int responseCode;
		
		HttpClient client = new DefaultHttpClient();
		
		try {
			HttpContext httpContext = sessionContainer.getSessionContext();

			List<NameValuePair> credentials = getCredentialsForPost();
			
			HttpPost post = new HttpPost(WARMSHOWERS_USER_AUTHENTICATION_URL);
			post.setEntity(new UrlEncodedFormEntity(credentials));
			HttpResponse response = client.execute(post, httpContext);
			
			HttpEntity entity = response.getEntity();
			
			responseCode = response.getStatusLine().getStatusCode();
            
            // Consume response content
            EntityUtils.toString(entity);
		}
		
		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}
		
		finally {
			client.getConnectionManager().shutdown();
		}
		
		if (responseCode != HttpStatus.SC_OK) {
			throw new HttpAuthenticationFailedException("Invalid credentials");
		}
		
		authenticated = true;
	}

	private List<NameValuePair> getCredentialsForPost() {
		List<NameValuePair> args = new ArrayList<NameValuePair>();
		args.add(new BasicNameValuePair("op", "Log in"));
		args.add(new BasicNameValuePair("form_id", "user_login"));
		args.add(new BasicNameValuePair("name", credentialsService.getUsername()));
		args.add(new BasicNameValuePair("pass", credentialsService.getPassword()));
		return args;
	}
}