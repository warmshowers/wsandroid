package fi.bitrite.android.ws.activity;

import roboguice.activity.RoboMapActivity;
import roboguice.inject.InjectView;
import android.os.Bundle;

import com.google.android.maps.MapView;

import fi.bitrite.android.ws.R;

public class MapTabActivity extends RoboMapActivity {

	@InjectView(R.id.mapView) MapView mapView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_tab);
		mapView.setBuiltInZoomControls(true);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
