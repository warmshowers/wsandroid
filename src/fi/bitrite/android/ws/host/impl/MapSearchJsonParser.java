package fi.bitrite.android.ws.host.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpException;
import roboguice.util.Strings;

import java.util.ArrayList;
import java.util.List;

public class MapSearchJsonParser {

    private final String json;

    public MapSearchJsonParser(String json) {
        this.json = json;
    }

    public List<HostBriefInfo> getHosts() {
        try {
            JsonParser parser = new JsonParser();
            JsonObject jsonObj = parser.parse(json).getAsJsonObject();

            if (!isComplete(jsonObj)) {
                throw new IncompleteResultsException("Could not retrieve hosts. Try again.");
            }

            return parseHosts(jsonObj);
        }

        catch (HttpException e) {
            throw e;
        }
        
        catch (Exception e) {
            throw new HttpException(e);
        }
    }

    private boolean isComplete(JsonObject jsonObj) throws Exception {
        String status = jsonObj.getAsJsonObject("status").get("status").getAsString();
        boolean isComplete = status.equals("complete");
        return isComplete;
    }


    private int getNumHosts(JsonObject jsonObj) throws Exception {
        return jsonObj.getAsJsonObject("status").get("totalresults").getAsInt();
    }

    private List<HostBriefInfo> parseHosts(JsonObject jsonObj) throws Exception {
        List<HostBriefInfo> hostList = new ArrayList<HostBriefInfo>();

        JsonArray hosts = jsonObj.getAsJsonArray("accounts");
        for (JsonElement host : hosts) {
            JsonObject hostObj = host.getAsJsonObject();

            int id = hostObj.get("uid").getAsInt();

            String name = hostObj.get("name").getAsString();
            if (Strings.isEmpty(name)) {
                name = "(Unknown host)";
            }

            StringBuilder location = new StringBuilder();
            location.append(hostObj.get("street").getAsString());
            if (location.length() > 0) {
                location.append(", ");
            }

            location.append(
                hostObj.get("city").getAsString()).append(", ")
                    .append(hostObj.get("province").getAsString().toUpperCase());

            if (hostObj.get("postal_code").getAsString().length() > 0 && 0 != hostObj.get("postal_code").getAsString().compareToIgnoreCase("none")) {
                location.append(" " + hostObj.get("postal_code").getAsString());
            }

            String lat = hostObj.get("latitude").getAsString();
            String lon = hostObj.get("longitude").getAsString();

            HostBriefInfo h = new HostBriefInfo(id, null, name, location.toString(), null);
            h.setLatitude(lat);
            h.setLongitude(lon);
            hostList.add(h);
        }

        return hostList;
    }
    
}
