package fi.bitrite.android.ws.activity;

import java.util.Collections;
import java.util.List;

import roboguice.activity.RoboMapActivity;
import roboguice.inject.InjectView;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayItem;
import de.android1.overlaymanager.OverlayManager;
import de.android1.overlaymanager.lazyload.LazyLoadCallback;
import de.android1.overlaymanager.lazyload.LazyLoadException;
import fi.bitrite.android.ws.R;

public class MapTabActivity extends RoboMapActivity {

	private static final String HOST_OVERLAY = "hostoverlay";

	protected static final int NUM_HOSTS_CUTOFF = 100;

	@InjectView(R.id.mapView) MapView mapView;
	@InjectView(R.id.lblBigNumber) TextView lblBigNumber;
	@InjectView(R.id.lblStatusMessage) TextView lblStatusMessage;

	OverlayManager overlayManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_tab);
		mapView.setBuiltInZoomControls(true);
		overlayManager = new OverlayManager(this, mapView);
	}

	@Override
	public void onStart() {
		super.onStart();

		Drawable defaultMarker = getResources().getDrawable(R.drawable.home);
		ManagedOverlay managedOverlay = overlayManager.createOverlay(HOST_OVERLAY, defaultMarker);

		managedOverlay.setLazyLoadCallback(new LazyLoadCallback() {
			public List<ManagedOverlayItem> lazyload(GeoPoint arg0, GeoPoint arg1, ManagedOverlay arg2)
					throws LazyLoadException {
				try {
					final int numHosts = 10000;

					hideBigNumber();
					sendMessage("Loading hosts ...", false);
					
					Thread.sleep(2000);
					
					if (numHosts > NUM_HOSTS_CUTOFF) {
						showBigNumber(numHosts);
						sendMessage("Too many hosts in area. Try zooming.", true);
					}
				}

				catch (Exception e) {
					throw new LazyLoadException(e);
				}

				return Collections.emptyList();
			}

			private void hideBigNumber() {
				mapView.post(new Runnable() {
					public void run() {
						hideBigNumberOfHosts();
					}
				});
			}
			
			private void showBigNumber(final int numHosts) {
				mapView.post(new Runnable() {
					public void run() {
						showBigNumberOfHosts(numHosts);
					}
				});
			}

			private void sendMessage(final String message, final boolean error) {
				mapView.post(new Runnable() {
					public void run() {
						updateStatusMessage(message, error);							}
				});
			}
		});

		// registers the ManagedOverlayer to the MapView
		overlayManager.populate();
		managedOverlay.invokeLazyLoad(500);
	}
	
	private void updateStatusMessage(String message, boolean error) {
		if (error) {
			lblStatusMessage.setTextColor(0xFFFF0000);
		} else {
			lblStatusMessage.setTextColor(0xFF000000);
		}

		lblStatusMessage.setText(message);
	}
	
	private void showBigNumberOfHosts(int numHosts) {
		lblBigNumber.setText(Integer.toString(numHosts));
		lblBigNumber.setVisibility(View.VISIBLE);
	}

	private void hideBigNumberOfHosts() {
		lblBigNumber.setVisibility(View.GONE);
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
