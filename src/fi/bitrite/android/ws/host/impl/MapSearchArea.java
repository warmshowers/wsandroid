package fi.bitrite.android.ws.host.impl;

import com.google.android.maps.GeoPoint;

public class MapSearchArea {

	public float minLat;
	public float minLon;
	public float maxLat;
	public float maxLon;
	public float centerLat;
	public float centerLon;

	public static MapSearchArea fromGeoPoints(GeoPoint topLeft, GeoPoint bottomRight) {
		MapSearchArea area = new MapSearchArea();
		area.minLat = bottomRight.getLatitudeE6() / 1.0e6f;
		area.minLon = topLeft.getLongitudeE6() / 1.0e6f;
		area.maxLat = topLeft.getLatitudeE6() / 1.0e6f;
		area.maxLon = bottomRight.getLongitudeE6() / 1.0e6f;
		area.centerLat = (area.minLat + area.maxLat) / 2.0f;
		area.centerLon = (area.minLon + area.maxLon) / 2.0f;
		return area;
	}
	
}
