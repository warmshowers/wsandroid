package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.api.HttpReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import roboguice.util.Strings;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.util.http.HttpException;

/**
 * Gets host information based on host ID.
 */
public class HttpHostInformation extends HttpReader {

    public HttpHostInformation(HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer) {
        super(authenticationService, sessionContainer);
    }

    public Host getHostInformation(int id) {
		String simpleUrl = new StringBuilder().append("http://www.warmshowers.org/user/")
		.append(id).append("/json").toString();
		String json = getPage(simpleUrl);

		try {
			JSONArray hostJsonArray = new JSONObject(json).getJSONArray("users");
			JSONObject hostJson = hostJsonArray.getJSONObject(0);
			Host host = Host.CREATOR.parse(hostJson.getJSONObject("user"));

			if (Strings.isEmpty(host.getFullname())) {
				throw new HttpException("Could not parse JSON");
			}

			return host;
		} catch (JSONException e) {
			throw new HttpException(e);
		}
	}
}
