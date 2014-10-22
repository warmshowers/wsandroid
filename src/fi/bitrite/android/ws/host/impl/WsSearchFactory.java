package fi.bitrite.android.ws.host.impl;
import com.google.android.gms.maps.model.LatLng;

import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.SearchFactory;

public class WsSearchFactory implements SearchFactory {

    public Search createTextSearch(String text) {
        return new HttpTextSearch(text);
    }

    public Search createMapSearch(LatLng northeast, LatLng southwest, int numHostsCutoff) {
        return new RestMapSearch(northeast, southwest);
    }

}
