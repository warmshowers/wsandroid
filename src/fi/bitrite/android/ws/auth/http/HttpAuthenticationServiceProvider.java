package fi.bitrite.android.ws.auth.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.AuthenticationService;
import fi.bitrite.android.ws.util.http.HttpUtils;

/**
 * Responsible for authenticating the user against the WarmShowers web service.
 */
@Singleton
public class HttpAuthenticationServiceProvider implements HttpAuthenticationService {

	private static final String WARMSHOWERS_USER_AUTHENTICATION_URL = "http://www.warmshowers.org/user";

	private static final String WARMSHOWERS_USER_AUTHENTICATION_TEST_URL = "http://www.warmshowers.org/search/wsuser";

	@Inject
	HttpSessionContainer sessionContainer;

	String username;
	String authtoken;

	/**
	 * Load a page in order to see if we are authenticated
	 */
	public boolean isAuthenticated() {
		HttpClient client = HttpUtils.getDefaultClient();
		int responseCode;
		try {
			String url = HttpUtils.encodeUrl(WARMSHOWERS_USER_AUTHENTICATION_TEST_URL);
			HttpGet get = new HttpGet(url);
			HttpContext context = sessionContainer.getSessionContext();

			HttpResponse response = client.execute(get, context);
			HttpEntity entity = response.getEntity();
			responseCode = response.getStatusLine().getStatusCode();
			EntityUtils.toString(entity);
		}

		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}
		
		return (responseCode == HttpStatus.SC_OK);
	}

	public void authenticate() {
		try {
			getCredentialsFromAccount();
			authenticate(username, authtoken);
		}
		
		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}
	}

	private void getCredentialsFromAccount() throws OperationCanceledException, AuthenticatorException, IOException {
		AccountManager accountManager = AccountManager.get(WSAndroidApplication.getAppContext());
		Account account = AuthenticationHelper.getWarmshowersAccount();

		authtoken = accountManager.blockingGetAuthToken(account, AuthenticationService.ACCOUNT_TYPE, true);
		username = account.name;
	}
	
	public void authenticate(String username, String password) {
		HttpClient client = HttpUtils.getDefaultClient();
		HttpContext httpContext = sessionContainer.getSessionContext();
		CookieStore cookieStore = (CookieStore) httpContext.getAttribute(ClientContext.COOKIE_STORE);
		cookieStore.clear();
		
		try {
			List<NameValuePair> credentials = generateCredentialsForPost(username, password);
			HttpPost post = new HttpPost(WARMSHOWERS_USER_AUTHENTICATION_URL);
			post.setEntity(new UrlEncodedFormEntity(credentials));
			HttpResponse response = client.execute(post, httpContext);

			HttpEntity entity = response.getEntity();

			// Consume response content
			EntityUtils.toString(entity);
		}

		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}

		if (!isAuthenticated()) {
			throw new HttpAuthenticationFailedException("Invalid credentials");
		}
	}

	private List<NameValuePair> generateCredentialsForPost(String username, String password) {
		List<NameValuePair> args = new ArrayList<NameValuePair>();
		args.add(new BasicNameValuePair("op", "Log in"));
		args.add(new BasicNameValuePair("form_id", "user_login"));
		args.add(new BasicNameValuePair("name", username));
		args.add(new BasicNameValuePair("pass", password));
		return args;
	}
}