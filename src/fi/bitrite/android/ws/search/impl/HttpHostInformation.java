package fi.bitrite.android.ws.search.impl;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.util.http.HttpException;
import fi.bitrite.android.ws.util.http.HttpUtils;

public class HttpHostInformation {

	private HttpAuthenticationService authenticationService;
	private HttpSessionContainer sessionContainer;

	private boolean authenticationPerformed;

	public HttpHostInformation(HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer) {
		this.authenticationService = authenticationService;
		this.sessionContainer = sessionContainer;
	}

	public int getHostId(String hostName) {
		authenticationPerformed = false;
		String html = getHostHtml(hostName);
		HttpHostIdScraper scraper = new HttpHostIdScraper(html);
		return scraper.getId();
	}

	private String getHostHtml(String hostName) {
		HttpClient client = HttpUtils.getDefaultClient();
		String html;
		int responseCode;

		try {
			String searchUrl = HttpUtils.encodeUrl(new StringBuilder("http://www.warmshowers.org/users/").append(
					hostName).toString());
			HttpGet get = new HttpGet(searchUrl);
			HttpContext context = sessionContainer.getSessionContext();

			HttpResponse response = client.execute(get, context);
			HttpEntity entity = response.getEntity();
			responseCode = response.getStatusLine().getStatusCode();

			html = EntityUtils.toString(entity, "UTF-8");
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
				html = getHostHtml(hostName);
			} else {
				throw new HttpException("Couldn't authenticate user");
			}
		}

		return html;
	}

	public Host getHostInformation(int id) {
		authenticationPerformed = false;
		String json = getHostJson(id);
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

	public String getHostJson(int id) {
		HttpClient client = HttpUtils.getDefaultClient();
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
				json = getHostJson(id);
			} else {
				throw new HttpException("Couldn't authenticate user");
			}
		}

		return json;
	}

}
