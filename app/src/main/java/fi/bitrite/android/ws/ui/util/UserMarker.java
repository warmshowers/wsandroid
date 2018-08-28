package fi.bitrite.android.ws.ui.util;

import android.content.Context;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import fi.bitrite.android.ws.model.SimpleUser;

public class UserMarker extends Marker {
    private final SimpleUser mUser;

    public UserMarker(Context context, MapView mapView, SimpleUser user) {
        super(mapView, context);

        mUser = user;
        setPosition(new GeoPoint(user.location.getLatitude(), user.location.getLongitude()));
    }

    public SimpleUser getUser() {
        return mUser;
    }
}
