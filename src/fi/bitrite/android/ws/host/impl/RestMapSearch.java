package fi.bitrite.android.ws.host.impl;

import com.google.android.maps.GeoPoint;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.model.HostBriefInfo;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class RestMapSearch extends RestClient implements Search {

    private static final String WARMSHOWERS_MAP_SEARCH_URL = "http://www.warmshowers.org/services/rest/hosts/by_location";

    private final int numHostsCutoff;
    private final MapSearchArea searchArea;

    public RestMapSearch(GeoPoint topLeft, GeoPoint bottomRight, int numHostsCutoff) {
        this.searchArea = MapSearchArea.fromGeoPoints(topLeft, bottomRight);
        this.numHostsCutoff = numHostsCutoff; 
    }

    public List<HostBriefInfo> doSearch() {
        // The map search works even if we're not authenticated,
        // but it returns less data. Easier to check first using
        // a simple GET
        if (!isAuthenticationPerformed()) {
            authenticate();
        }

        String json = getHostsJson();
        return new MapSearchJsonParser(json, numHostsCutoff).getHosts();
    }

    private String getHostsJson() {
        return getJson(WARMSHOWERS_MAP_SEARCH_URL, getSearchParameters());
    }

    private List<NameValuePair> getSearchParameters() {
        List<NameValuePair> args = new ArrayList<NameValuePair>();
        args.add(new BasicNameValuePair("minlat", String.valueOf(searchArea.minLat)));
        args.add(new BasicNameValuePair("maxlat", String.valueOf(searchArea.maxLat)));
        args.add(new BasicNameValuePair("minlon", String.valueOf(searchArea.minLon)));
        args.add(new BasicNameValuePair("maxlon", String.valueOf(searchArea.maxLon)));
        args.add(new BasicNameValuePair("centerlat", String.valueOf(searchArea.centerLat)));
        args.add(new BasicNameValuePair("centerlon", String.valueOf(searchArea.centerLon)));
        args.add(new BasicNameValuePair("limit", String.valueOf(this.numHostsCutoff)));
        return args;
    }

}
