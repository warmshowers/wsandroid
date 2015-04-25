package fi.bitrite.android.ws.host.impl;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpException;

import java.util.ArrayList;
import java.util.List;

public class MapSearchJsonParser {

    private static final String TAG = "MapSearchJsonParser";
    private final JSONObject mJSONObj;

    public MapSearchJsonParser(JSONObject json) {
        mJSONObj = json;
    }

    public List<HostBriefInfo> getHosts() throws HttpAuthenticationFailedException, HttpException, JSONException {
        if (!isComplete(mJSONObj)) {
            throw new IncompleteResultsException("Could not retrieve hosts. Try again.");
        }
        return parseHosts(mJSONObj);
    }

    private boolean isComplete(JSONObject jsonObj) throws JSONException {
        String status = jsonObj.getJSONObject("status").getString("status");
        boolean isComplete = status.equals("complete");
        return isComplete;
    }


    private int getNumHosts(JSONObject jsonObj) throws JSONException {
        JSONObject status = jsonObj.getJSONObject("status");
        return status.getInt("totalresults");
    }

    private List<HostBriefInfo> parseHosts(JSONObject jsonObj) throws JSONException{
        List<HostBriefInfo> hostList = new ArrayList<HostBriefInfo>();

        JSONArray hosts = jsonObj.getJSONArray("accounts");
        for (int i=0; i < hosts.length(); i++) {
            JSONObject hostObj = hosts.getJSONObject(i);

            int id = hostObj.getInt("uid");

            String fullName = hostObj.getString("name");
            if (fullName.isEmpty()) {
                fullName = "(Unknown host)";
            }

            StringBuilder location = new StringBuilder();
            location.append(hostObj.getString("street"));
            if (location.length() > 0) {
                location.append(", ");
            }

            if (hostObj.getString("postal_code").length() > 0 && 0 != hostObj.getString("postal_code").compareToIgnoreCase("none")) {
                location.append(" " + hostObj.getString("postal_code"));
            }

            String lat = hostObj.getString("latitude");
            String lon = hostObj.getString("longitude");

            HostBriefInfo h = new HostBriefInfo(
                    id,
                    "",  // No username provided in this feed
                    fullName,
                    hostObj.getString("street"),
                    hostObj.getString("city"),
                    hostObj.getString("province"),
                    hostObj.getString("country"),
                    "",   // No about_me provided here
                    (hostObj.getString("notcurrentlyavailable").equals("1")),
                    hostObj.getString("access"),
                    hostObj.getString("created")
            );
            h.setLatitude(lat);
            h.setLongitude(lon);
            hostList.add(h);
        }

        return hostList;
    }

}
