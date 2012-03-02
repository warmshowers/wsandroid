package fi.bitrite.android.ws.auth.http;

import java.io.IOException;
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.AuthenticationService;

/**
 * Responsible for authenticating the user against the WarmShowers web service.
 */
@Singleton
public class HttpAuthenticationServiceProvider implements HttpAuthenticationService {

	private static final String WARMSHOWERS_USER_AUTHENTICATION_URL = "http://www.warmshowers.org/user";

	@Inject
	HttpSessionContainer sessionContainer;

	boolean authenticated = false;
	
	String username;
	String authtoken;

	public boolean isAuthenticated() {
		// TODO: do a HTTP GET to see if we are authenticated?
		return authenticated;
	}

	public void authenticate() {
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

	private List<NameValuePair> getCredentialsForPost() throws AuthenticatorException, IOException, OperationCanceledException {
		AccountManager accountManager = AccountManager.get(WSAndroidApplication.getAppContext());
		Account account = AuthenticationHelper.getWarmshowersAccount();

		authtoken = accountManager.blockingGetAuthToken(account, AuthenticationService.ACCOUNT_TYPE, true);
		username = account.name;
		
		List<NameValuePair> args = new ArrayList<NameValuePair>();
		args.add(new BasicNameValuePair("op", "Log in"));
		args.add(new BasicNameValuePair("form_id", "user_login"));
		args.add(new BasicNameValuePair("name", username));
		args.add(new BasicNameValuePair("pass", authtoken));
		return args;
	}
	
	/*
	private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
	    public void run(AccountManagerFuture<Bundle> result) {
	        // Get the result of the operation from the AccountManagerFuture.
			try {
				Bundle bundle = result.getResult();
		        username = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
				authtoken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
		        doAuthenticationPost();
			} 
			catch (Exception e) {
				Log.e("WSAndroid", e.toString());
			}
	    }
	}

	private void doAuthenticationPost() {
		// TODO Auto-generated method stub
		
	}
	*/

}