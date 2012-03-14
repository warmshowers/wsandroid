package fi.bitrite.android.ws.contact;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.util.http.HttpUtils;

public class HttpHostContact {
	
	private HttpAuthenticationService authenticationService;
	private HttpSessionContainer sessionContainer;
	
	public HttpHostContact(HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer) {
		this.authenticationService = authenticationService;
		this.sessionContainer = sessionContainer;
	}
	
	public void send(String name, String subject, String message) {
		if (!authenticationService.isAuthenticated()) {
			authenticationService.authenticate();
		}
		
		sendMessage(name, subject, message);
	}

	private void sendMessage(String name, String subject, String message) {
		HttpClient client = HttpUtils.getDefaultClient();
		try {
			String url = HttpUtils.encodeUrl(new StringBuilder().append("http://www.warmshowers.org/users/")
					.append(name).append("/contact").toString());
			
			List<NameValuePair> params = generatePostParameters(subject, message);
			HttpPost post = new HttpPost(url);
			post.setEntity(new UrlEncodedFormEntity(params));
			HttpContext context = sessionContainer.getSessionContext();			
			HttpResponse response = client.execute(post, context);
			HttpEntity entity = response.getEntity();
			
			// Consume response content
			String html = EntityUtils.toString(entity);
			System.out.println(html);
		}

		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}
	}

	private List<NameValuePair> generatePostParameters(String subject, String message) {
		List<NameValuePair> args = new ArrayList<NameValuePair>();
		args.add(new BasicNameValuePair("op", "Send e-mail"));
		args.add(new BasicNameValuePair("form_id", "contact_mail_user"));
		args.add(new BasicNameValuePair("form_token", "bef3c8c1fe6440bd1adb8446125c15c4"));
		args.add(new BasicNameValuePair("subject", subject));
		args.add(new BasicNameValuePair("message", message));
		return args;
	}

}
