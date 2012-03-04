package fi.bitrite.android.ws.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import roboguice.activity.RoboMapActivity;
import roboguice.inject.InjectView;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.inject.Inject;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayItem;
import de.android1.overlaymanager.OverlayManager;
import de.android1.overlaymanager.lazyload.LazyLoadCallback;
import de.android1.overlaymanager.lazyload.LazyLoadException;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.search.Search;
import fi.bitrite.android.ws.search.SearchFactory;
import fi.bitrite.android.ws.search.impl.SearchFailedException;
import fi.bitrite.android.ws.search.impl.TooManyHostsException;

public class MapTabActivity extends RoboMapActivity {

	private static final String HOST_OVERLAY = "hostoverlay";

	protected static final int NUM_HOSTS_CUTOFF = 100;

	@InjectView(R.id.mapView) MapView mapView;
	@InjectView(R.id.lblBigNumber) TextView lblBigNumber;
	@InjectView(R.id.lblStatusMessage) TextView lblStatusMessage;

	@Inject SearchFactory searchFactory;
	
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

		Drawable marker = getResources().getDrawable(R.drawable.shower);
		ManagedOverlay managedOverlay = overlayManager.createOverlay(HOST_OVERLAY, marker);

		managedOverlay.setLazyLoadCallback(new LazyLoadCallback() {
			public List<ManagedOverlayItem> lazyload(GeoPoint topLeft, GeoPoint bottomRight, ManagedOverlay overlay)
					throws LazyLoadException {
				List<ManagedOverlayItem> overlayItems = new ArrayList<ManagedOverlayItem>();
				try {
					
					hideBigNumber();
					sendMessage("Loading hosts ...", false);

					Search search = searchFactory.createMapSearch(topLeft, bottomRight, NUM_HOSTS_CUTOFF);
					
					try {
						List<HostBriefInfo> hosts = search.doSearch();
						sendMessage(hosts.size() + " hosts in area", false);
						ListIterator<HostBriefInfo> hostIter = hosts.listIterator();
						while (hostIter.hasNext()) {
							HostBriefInfo host = hostIter.next();
							GeoPoint point = new GeoPoint((int) Math.round(new Float(host.getLatitude()).floatValue() * 1e6), (int) Math.round(new Float(host.getLongitude()).floatValue() * 1e6));
							overlayItems.add(new ManagedOverlayItem(point, "title", "snippet"));
						}
					}

					catch (TooManyHostsException e) {
						int n = e.getNumHosts();
						showBigNumber((n > 1000) ? "1000+" : new Integer(n).toString());
						sendMessage("Too many hosts in area. Try zooming.", true);
					}
					
					catch (SearchFailedException e) {
						Log.e("WSAndroid", e.getMessage(), e);
						sendMessage(e.getMessage(), true);
					}
				}

				catch (Exception e) {
					throw new LazyLoadException(e);
				}

				return overlayItems;
			}

			private void hideBigNumber() {
				mapView.post(new Runnable() {
					public void run() {
						hideBigNumberOfHosts();
					}
				});
			}

			private void showBigNumber(final String number) {
				mapView.post(new Runnable() {
					public void run() {
						showBigNumberOfHosts(number);
					}
				});
			}

			private void sendMessage(final String message, final boolean error) {
				mapView.post(new Runnable() {
					public void run() {
						updateStatusMessage(message, error);
					}
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

	private void showBigNumberOfHosts(String number) {
		lblBigNumber.setText(number);
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
