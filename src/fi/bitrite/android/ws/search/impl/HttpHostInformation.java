package fi.bitrite.android.ws.search.impl;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.util.http.HttpUtils;

public class HttpHostInformation {

	private int id;
	private HttpAuthenticationService authenticationService;
	private HttpSessionContainer sessionContainer;

	private boolean authenticationPerformed;

	public HttpHostInformation(int id, HttpAuthenticationService authenticationService,
			HttpSessionContainer sessionContainer) {
		this.id = id;
		this.authenticationService = authenticationService;
		this.sessionContainer = sessionContainer;
	}

	public Host getHostInformation() {
		authenticationPerformed = false;
		String json = getHostJson();
		try {
			JSONArray hostJsonArray = new JSONObject(json).getJSONArray("users");
			JSONObject hostJson = hostJsonArray.getJSONObject(0);
			Host host = Host.CREATOR.parse(hostJson.getJSONObject("user"));
			
			if (host.getFullname().isEmpty()) {
				throw new HttpException("Could not parse JSON");
			}
			
			return host;
		} catch (JSONException e) {
			throw new HttpException(e);
		}
	}

	public String getHostJson() {
		HttpClient client = new DefaultHttpClient();
		String json;
		int responseCode;

		try {
			String searchUrl = HttpUtils.encodeUrl(new StringBuilder().append("http://www.warmshowers.org/user/")
					.append(id).append("/json").toString());
			HttpGet get = new HttpGet(searchUrl);
			HttpContext context = sessionContainer.getSessionContext();

			HttpResponse response = client.execute(get, context);
			HttpEntity entity = response.getEntity();
			responseCode = response.getStatusLine().getStatusCode();

			json = EntityUtils.toString(entity, "UTF-8");
		} catch (Exception e) {
			throw new HttpException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}

		if (responseCode == HttpStatus.SC_FORBIDDEN) {
			if (!authenticationPerformed) {
				authenticationService.authenticate();
				authenticationPerformed = true;
				json = getHostJson();
			} else {
				throw new HttpException("Couldn't authenticate user");
			}
		}

		return json;
	}

}
