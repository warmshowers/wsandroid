package fi.bitrite.android.ws.host;

import com.google.android.maps.GeoPoint;

public interface SearchFactory {

    public Search createTextSearch(String keyword);
    public Search createMapSearch(GeoPoint topLeft, GeoPoint bottomRight, int numHostsCutoff);

}