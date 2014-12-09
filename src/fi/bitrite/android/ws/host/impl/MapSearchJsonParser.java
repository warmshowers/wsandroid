package fi.bitrite.android.ws.host.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
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

    public List<HostBriefInfo> getHosts() throws HttpAuthenticationFailedException, HttpException {
        try {
            JsonParser parser = new JsonParser();
            JsonObject jsonObj = parser.parse(json).getAsJsonObject();

            if (!isComplete(jsonObj)) {
                throw new IncompleteResultsException("Could not retrieve hosts. Try again.");
            }

            return parseHosts(jsonObj);
        } catch (com.google.gson.JsonIOException e) {
            throw new HttpException(e);
        } catch (com.google.gson.JsonSyntaxException e) {
            throw new HttpException(e);
        }
    }

    private boolean isComplete(JsonObject jsonObj) {
        String status = jsonObj.getAsJsonObject("status").get("status").getAsString();
        boolean isComplete = status.equals("complete");
        return isComplete;
    }


    private int getNumHosts(JsonObject jsonObj) {
        return jsonObj.getAsJsonObject("status").get("totalresults").getAsInt();
    }

    private List<HostBriefInfo> parseHosts(JsonObject jsonObj) {
        List<HostBriefInfo> hostList = new ArrayList<HostBriefInfo>();

        JsonArray hosts = jsonObj.getAsJsonArray("accounts");
        for (JsonElement host : hosts) {
            JsonObject hostObj = host.getAsJsonObject();

            int id = hostObj.get("uid").getAsInt();

            String fullName = hostObj.get("name").getAsString();
            if (Strings.isEmpty(fullName)) {
                fullName = "(Unknown host)";
            }

            StringBuilder location = new StringBuilder();
            location.append(hostObj.get("street").getAsString());
            if (location.length() > 0) {
                location.append(", ");
            }

            if (hostObj.get("postal_code").getAsString().length() > 0 && 0 != hostObj.get("postal_code").getAsString().compareToIgnoreCase("none")) {
                location.append(" " + hostObj.get("postal_code").getAsString());
            }

            String lat = hostObj.get("latitude").getAsString();
            String lon = hostObj.get("longitude").getAsString();

            HostBriefInfo h = new HostBriefInfo(
                    id,
                    "",  // No username provided in this feed
                    fullName,
                    hostObj.get("street").getAsString(),
                    hostObj.get("city").getAsString(),
                    hostObj.get("province").getAsString(),
                    hostObj.get("country").getAsString(),
                    "",   // No about_me provided here
                    (hostObj.get("notcurrentlyavailable").getAsString().equals("1"))
            );
            h.setLatitude(lat);
            h.setLongitude(lon);
            hostList.add(h);
        }

        return hostList;
    }

}
