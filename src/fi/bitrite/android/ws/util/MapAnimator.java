package fi.bitrite.android.ws.util;

import android.content.Intent;

import com.google.android.maps.GeoPoint;
import com.google.inject.Singleton;

/**
 * Helps out when we want to zoom the map to a host's location from the host information page.
 */
@Singleton
public class MapAnimator {

	private GeoPoint target;
	
	public MapAnimator() {
		clearTarget();
	}

	public void prepareToAnimateToHost(Intent data) {
		int lat = data.getIntExtra("lat", 0);
		int lon = data.getIntExtra("lon", 0);
		target = new GeoPoint(lat, lon);
	}

	public GeoPoint getTarget() {
		return target;
	}

	public void clearTarget() {
		target = null;
	}
	
}
