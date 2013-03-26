package fi.bitrite.android.ws.host.impl;

import com.google.android.maps.GeoPoint;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.SearchFactory;

public class WsSearchFactory implements SearchFactory {

    public Search createTextSearch(String text) {
        return new HttpTextSearch(text);
    }

    public Search createMapSearch(GeoPoint topLeft, GeoPoint bottomRight, int numHostsCutoff) {
        return new RestMapSearch(topLeft, bottomRight, numHostsCutoff);
    }

}
