package fi.bitrite.android.ws.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import roboguice.activity.RoboMapActivity;
import roboguice.inject.InjectView;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.gson.Gson;
import com.google.inject.Inject;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayGestureDetector;
import de.android1.overlaymanager.ManagedOverlayGestureDetector.OnOverlayGestureListener;
import de.android1.overlaymanager.ManagedOverlayItem;
import de.android1.overlaymanager.OverlayManager;
import de.android1.overlaymanager.ZoomEvent;
import de.android1.overlaymanager.lazyload.LazyLoadCallback;
import de.android1.overlaymanager.lazyload.LazyLoadException;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.search.Search;
import fi.bitrite.android.ws.search.SearchFactory;
import fi.bitrite.android.ws.search.impl.HttpException;
import fi.bitrite.android.ws.search.impl.TooManyHostsException;

public class MapTabActivity extends RoboMapActivity {

	private static final String HOST_OVERLAY = "hostoverlay";

	// TODO: make this a user-definable setting
	protected static final int NUM_HOSTS_CUTOFF = 100;

	@InjectView(R.id.mapView) MapView mapView;
	@InjectView(R.id.lblBigNumber) TextView lblBigNumber;
	@InjectView(R.id.lblStatusMessage) TextView lblStatusMessage;

	@Inject SearchFactory searchFactory;

	MapController mapController;
	OverlayManager overlayManager;
	Gson gson;
	HostBriefInfo host;
	Dialog hostPopup;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_tab);
		mapView.setBuiltInZoomControls(true);
		overlayManager = new OverlayManager(this, mapView);
		mapController = mapView.getController();
		gson = new Gson();
		setupHostPopup();
	}

	private void setupHostPopup() {
		hostPopup = new Dialog(this);
		hostPopup.setContentView(R.layout.map_popup);
		hostPopup.setCancelable(true);

		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		lp.copyFrom(hostPopup.getWindow().getAttributes());
		lp.width = WindowManager.LayoutParams.FILL_PARENT;
		hostPopup.getWindow().setAttributes(lp);

		TextView close = (TextView) hostPopup.findViewById(R.id.lblMapPopupClose);
		close.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				hostPopup.dismiss();
			}
		});

		TextView details = (TextView) hostPopup.findViewById(R.id.lblMapPopupViewDetails);
		details.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				hostPopup.dismiss();
				Intent i = new Intent(MapTabActivity.this, HostInformationActivity.class);
				i.putExtra("host", Host.createFromBriefInfo(host));
				i.putExtra("host_id", host.getId());
				startActivity(i);
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();

		Drawable marker = getResources().getDrawable(R.drawable.shower);
		ManagedOverlay managedOverlay = overlayManager.createOverlay(HOST_OVERLAY, marker);
		managedOverlay.setLazyLoadCallback(createLazyLoadCallback());
		managedOverlay.setOnOverlayGestureListener(createOnOverlayGestureListener());

		// registers the ManagedOverlayer to the MapView
		overlayManager.populate();

		managedOverlay.invokeLazyLoad(500);
	}

	private LazyLoadCallback createLazyLoadCallback() {
		return new LazyLoadCallback() {
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
							GeoPoint point = getGeoPointFromHost(host);
							overlayItems.add(new ManagedOverlayItem(point, host.getFullname(), gson.toJson(host)));
						}
					}

					catch (TooManyHostsException e) {
						int n = e.getNumHosts();
						showBigNumber((n > 1000) ? "1000+" : new Integer(n).toString());
						sendMessage("Too many hosts in area. Try zooming.", true);
					}

					catch (HttpException e) {
						Log.e("WSAndroid", e.getMessage(), e);
						sendMessage(e.getMessage(), true);
					}
				}

				catch (Exception e) {
					throw new LazyLoadException(e);
				}

				return overlayItems;
			}

			private GeoPoint getGeoPointFromHost(HostBriefInfo host) {
				return new GeoPoint((int) Math.round(new Float(host.getLatitude()).floatValue() * 1e6),
						(int) Math.round(new Float(host.getLongitude()).floatValue() * 1e6));
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
		};

	}

	private OnOverlayGestureListener createOnOverlayGestureListener() {
		return new ManagedOverlayGestureDetector.OnOverlayGestureListener() {

			public boolean onSingleTap(MotionEvent e, ManagedOverlay overlay, GeoPoint point, ManagedOverlayItem item) {
				if (item != null) {
					showHostPopup(item.getTitle(), item.getSnippet());
				}
				return true;
			}

			public boolean onDoubleTap(MotionEvent e, ManagedOverlay overlay, GeoPoint point, ManagedOverlayItem item) {
				mapController.animateTo(point);
				mapController.zoomIn();
				return true;
			}

			public boolean onScrolled(MotionEvent e1, MotionEvent e2, float distX, float distY, ManagedOverlay overlay) {
				return false;
			}

			public boolean onZoom(ZoomEvent zoom, ManagedOverlay overlay) {
				return false;
			}

			public void onLongPress(MotionEvent e, ManagedOverlay overlay) {
			}

			public void onLongPressFinished(MotionEvent e, ManagedOverlay overlay, GeoPoint point,
					ManagedOverlayItem item) {
			}
		};
	}

	private void showHostPopup(String title, String snippet) {
		host = gson.fromJson(snippet, HostBriefInfo.class);
		hostPopup.setTitle(host.getFullname());
		TextView location = (TextView) hostPopup.findViewById(R.id.lblMapPopupLocation);
		location.setText(host.getLocation());
		hostPopup.show();
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
