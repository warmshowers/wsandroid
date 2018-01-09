package fi.bitrite.android.ws.host.impl;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.http.HttpException;

public class RestMapSearch extends RestClient implements Search {

    private static final String WARMSHOWERS_MAP_SEARCH_URL = GlobalInfo.warmshowersBaseUrl + "/services/rest/hosts/by_location";
    private int numHostsCutoff = 800;

    private final MapSearchArea searchArea;

    public RestMapSearch(AuthenticationController authenticationController,
                         LatLng northEast, LatLng southWest) {
        super(authenticationController);

        this.searchArea = MapSearchArea.fromLatLngs(northEast, southWest);
    }

    public List<Host> doSearch() throws JSONException, HttpException, IOException {
        JSONObject json = getHostsJson();
        return new MapSearchJsonParser(json).getHosts();
    }

    private JSONObject getHostsJson() throws JSONException, HttpException, IOException {
        return post(WARMSHOWERS_MAP_SEARCH_URL, getSearchParameters());
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
