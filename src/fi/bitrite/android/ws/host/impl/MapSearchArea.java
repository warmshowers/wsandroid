package fi.bitrite.android.ws.host.impl;

import com.google.android.gms.maps.model.LatLng;

public class MapSearchArea {

    public double minLat, minLon, maxLat, maxLon, centerLat, centerLon;

    public static MapSearchArea fromLatLngs(LatLng northEast, LatLng southWest) {
        MapSearchArea area = new MapSearchArea();
        area.minLat = southWest.latitude;
        area.minLon = southWest.longitude;
        area.maxLat = northEast.latitude;
        area.maxLon = northEast.longitude;
        area.centerLat = (area.minLat + area.maxLat) / 2.0f;
        area.centerLon = (area.minLon + area.maxLon) / 2.0f;
        return area;
    }

}
