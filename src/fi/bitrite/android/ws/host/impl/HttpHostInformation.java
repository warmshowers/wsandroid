package fi.bitrite.android.ws.host.impl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.util.http.HttpException;

public class HttpHostInformation extends HttpPageReader {

	public HttpHostInformation(HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer) {
		super(authenticationService, sessionContainer);
	}

	public int getHostId(String hostName) {
		String simpleUrl = new StringBuilder("http://www.warmshowers.org/users/").append(hostName).toString();
		String html = getPage(simpleUrl);
		HttpHostIdScraper scraper = new HttpHostIdScraper(html);
		return scraper.getId();
	}

	public Host getHostInformation(int id) {
		String simpleUrl = new StringBuilder().append("http://www.warmshowers.org/user/")
		.append(id).append("/json").toString();
		String json = getPage(simpleUrl);

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
}
