package fi.bitrite.android.ws.host.impl;

import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import fi.bitrite.android.ws.activity.AuthenticatorActivity;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.util.http.HttpUtils;

public class HttpHostContact extends HttpPageReader {

	private AccountManager accountManager;
	
	public HttpHostContact(HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer, AccountManager accountManager) {
		super(authenticationService, sessionContainer);
		this.accountManager = accountManager;
	}

	public void send(int id, String subject, String message) {
		Account account = AuthenticationHelper.getWarmshowersAccount();
		String accountUserId = accountManager.getUserData(account, AuthenticatorActivity.KEY_USERID);
		
		String contactFormUrl = new StringBuilder().append("http://www.warmshowers.org/user/").append(accountUserId)
				.append("/messages/new/").append(id).toString();

		String html = getPage(contactFormUrl);
		List<NameValuePair> formDetails = new HttpHostContactFormScraper(html).getFormDetails();
		formDetails.add(new BasicNameValuePair("subject", subject));
		formDetails.add(new BasicNameValuePair("body", message));
		sendMessageForm(contactFormUrl, formDetails);
	}

	private void sendMessageForm(String contactFormUrl, List<NameValuePair> formDetails) {
		HttpClient client = HttpUtils.getDefaultClient();
		try {
			String url = HttpUtils.encodeUrl(contactFormUrl);

			HttpPost post = new HttpPost(url);
			post.setEntity(new UrlEncodedFormEntity(formDetails));
			HttpContext context = sessionContainer.getSessionContext();
			HttpResponse response = client.execute(post, context);
			HttpEntity entity = response.getEntity();

			// Consume response content
			EntityUtils.toString(entity, "UTF-8");
		}

		catch (ClientProtocolException e) {
			if (e.getCause() instanceof CircularRedirectException) {
				// If we get this the message has still been sent successfully, so ignore it
			} else {
				throw new HttpAuthenticationFailedException(e);
			}
		}
		
		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}
	}
}
