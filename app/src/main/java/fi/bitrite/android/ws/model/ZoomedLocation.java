package fi.bitrite.android.ws.model;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;

public class ZoomedLocation {
    public final IGeoPoint location;
    public final double zoom;

    public ZoomedLocation(double lat, double lon, double zoom) {
        this(new GeoPoint(lat, lon), zoom);
    }
    public ZoomedLocation(IGeoPoint location, double zoom) {
        this.location = location;
        this.zoom = zoom;
    }
}
