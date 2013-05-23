package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.api.HttpReader;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.util.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import roboguice.util.Strings;

/**
 * Gets host information based on host ID.
 */
public class HttpHostInformation extends HttpReader {

    public Host getHostInformation(int id) {
        String simpleUrl = new StringBuilder().append("https://www.warmshowers.org/user/")
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
