package fi.bitrite.android.ws.host.impl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.util.http.HttpException;

public class MapSearchJsonParser {

    private static final String TAG = "MapSearchJsonParser";
    private final JSONObject mJSONObj;

    public MapSearchJsonParser(JSONObject json) {
        mJSONObj = json;
    }

    public List<Host> getHosts() throws HttpException, JSONException {
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

    private List<Host> parseHosts(JSONObject jsonObj) throws JSONException{
        List<Host> hostList = new ArrayList<Host>();

        JSONArray hosts = jsonObj.getJSONArray("accounts");
        for (int i=0; i < hosts.length(); i++) {
            JSONObject hostObj = hosts.getJSONObject(i);

            Host host = Host.CREATOR.parse(hostObj);
            hostList.add(host);
        }

        return hostList;
    }

}
