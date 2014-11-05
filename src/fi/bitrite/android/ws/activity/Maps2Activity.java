package fi.bitrite.android.ws.activity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;

import java.util.concurrent.ConcurrentHashMap;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.impl.RestMapSearch;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.WSNonHierarchicalDistanceBasedAlgorithm;
import fi.bitrite.android.ws.util.http.HttpException;


public class Maps2Activity extends FragmentActivity implements
        ClusterManager.OnClusterClickListener<HostBriefInfo>,
        ClusterManager.OnClusterInfoWindowClickListener<HostBriefInfo>,
        ClusterManager.OnClusterItemClickListener<HostBriefInfo>,
        ClusterManager.OnClusterItemInfoWindowClickListener<HostBriefInfo>,
        GoogleMap.OnCameraChangeListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private MapSearchTask searchTask;
    private ConcurrentHashMap<Integer, HostBriefInfo> mHosts = new ConcurrentHashMap<Integer, HostBriefInfo>();
    private ClusterManager<HostBriefInfo> mClusterManager;
    private Cluster<HostBriefInfo> mLastClickedCluster;
    private final static int  CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";

    LocationClient mLocationClient;
    private boolean mPlayServicesConnectionStatus = false;
    private static final String TAG = "Maps2Activity";
    private CameraPosition mLastCameraPosition = null;
    private boolean mResolvingError = false;
    Location mLastDeviceLocation;
    String mDistanceUnit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDistanceUnit = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("distance_unit", "km");

        setContentView(R.layout.activity_maps);

        mLocationClient = new LocationClient(this, this, this);

        setUpMapIfNeeded();
        mMap.setOnCameraChangeListener(this);

        CameraPosition position = getSavedCameraPosition();
        if (position != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
            // The move itself will end up setting the mlastCameraPosition.
        }

        mClusterManager = new ClusterManager<HostBriefInfo>(this, mMap);
        mClusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<HostBriefInfo>(new WSNonHierarchicalDistanceBasedAlgorithm<HostBriefInfo>()));
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);
        mClusterManager.setRenderer(new HostRenderer());
        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());
        mClusterManager.getClusterMarkerCollection().setOnInfoWindowAdapter(new ClusterInfoWindowAdapter(getLayoutInflater()));
        mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new SingleHostInfoWindowAdapter(getLayoutInflater()));
    }

    /**
     * This is where google play services gets connected and we can now find recent location.
     *
     * Note that all the complex stuff about connecting to Google Play Services (just to get location)
     * is from http://developer.android.com/training/location/retrieve-current.html and I don't actually
     * know how to test it.
     *
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected to location services mLastCameraPosition==" + (mLastCameraPosition != null));
        mPlayServicesConnectionStatus = true;

        mLastDeviceLocation = mLocationClient.getLastLocation();

        // If we are now connected, but still don't have a location, use a bogus default.
        if (mLastDeviceLocation == null) {
            mLastDeviceLocation = new Location("default");

            mLastDeviceLocation.setLatitude(
                    Double.parseDouble(getResources().getString(R.string.map_default_latitude)));
            mLastDeviceLocation.setLongitude(Double.parseDouble(getResources().getString(R.string.map_default_longitude)));
        }

        mMap.setMyLocationEnabled(true);

        if (getSavedCameraPosition() == null) {
            setMapToCurrentLocation();
        }
    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "Disconnected from location services");
        mPlayServicesConnectionStatus = false;
        Toast.makeText(this, "Disconnected from location services. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showErrorDialog(connectionResult.getErrorCode());
        }

    }

    /**
     * Add the title and snippet to the marker so that infoWindow can be rendered.
     */
    private class HostRenderer extends DefaultClusterRenderer<HostBriefInfo> {
        private final IconGenerator mIconGenerator = new IconGenerator(getApplicationContext());
        private final IconGenerator mClusterIconGenerator = new IconGenerator(getApplicationContext());
        private final ImageView mImageView;
        private final ImageView mClusterImageView;
        private final int mDimension;

        public HostRenderer() {
            super(getApplicationContext(), mMap, mClusterManager);

            View singleHostMarkerView = getLayoutInflater().inflate(R.layout.single_host_marker, null);
            mClusterIconGenerator.setContentView(singleHostMarkerView);
            mClusterImageView = (ImageView) singleHostMarkerView.findViewById(R.id.image);

            mImageView = new ImageView(getApplicationContext());
            mDimension = (int) getResources().getDimension(R.dimen.single_host_marker_size);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
            mImageView.setPadding(padding, padding, padding, padding);
            mIconGenerator.setContentView(mImageView);
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<HostBriefInfo> cluster, MarkerOptions markerOptions) {
            super.onBeforeClusterRendered(cluster, markerOptions);

            if (allItemsInSameLocation(cluster)) {
                Drawable drawable = getResources().getDrawable(R.drawable.map_markers_multiple);
                mClusterImageView.setImageDrawable(drawable);
                Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
            }
        }

        @Override
        protected void onBeforeClusterItemRendered(HostBriefInfo host, MarkerOptions markerOptions) {
            String street = host.getStreet();
            String snippet = host.getCity() + ", " + host.getProvince().toUpperCase();
            if (street != null && street.length() > 0) {
                snippet = street + "<br/>" + snippet;
            }
            if (mLastDeviceLocation != null) {
                double distance = Tools.calculateDistanceBetween(host.getLatLng(), mLastDeviceLocation, mDistanceUnit);
                snippet += "<br/>" + getString(R.string.distance_from_current, (int)distance, mDistanceUnit);
            }
            markerOptions.title(host.getFullname()).snippet(snippet);

            mImageView.setImageResource(R.drawable.map_markers_single);
            Bitmap icon = mIconGenerator.makeIcon();
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(host.getFullname());
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            return (cluster.getSize() > 1);
        }

        protected boolean allItemsInSameLocation(Cluster<HostBriefInfo> cluster) {
            boolean allInOnePlace = true;
            HostBriefInfo firstHost = (HostBriefInfo)cluster.getItems().toArray()[0];
            LatLng firstLatLng = firstHost.getLatLng();
            for (HostBriefInfo host: cluster.getItems()) {
                if (!host.getLatLng().equals(firstLatLng)) {
                    allInOnePlace = false;
                    break;
                }
            }
            return allInOnePlace;
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.i(TAG, "onRestoreInstanceState, savedInstanceState==" + (savedInstanceState != null));

    }



    class ClusterInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private View mPopup=null;
        private LayoutInflater mInflater=null;

        ClusterInfoWindowAdapter(LayoutInflater inflater) {
            this.mInflater = inflater;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {

            String hostList = "";
            ArrayList<HostBriefInfo> hosts = new ArrayList<HostBriefInfo>();
            if (mPopup == null) {
                mPopup = mInflater.inflate(R.layout.info_window, null);
            }
            TextView tv = (TextView)mPopup.findViewById(R.id.title);

            if (mLastClickedCluster != null) {

                if (mLastDeviceLocation != null) {
                    double distance = Tools.calculateDistanceBetween(marker.getPosition(), mLastDeviceLocation, mDistanceUnit);
                    TextView distance_tv = (TextView)mPopup.findViewById(R.id.distance_from_current);
                    distance_tv.setText(Html.fromHtml(getString(R.string.distance_from_current, (int)distance, mDistanceUnit)));
                }

                hosts = (ArrayList<HostBriefInfo>) mLastClickedCluster.getItems();
                if (mLastClickedCluster != null) {
                    for (HostBriefInfo host : hosts) {
                        hostList += host.getFullname() + "<br/>";
                    }

                    hostList += getString(R.string.click_to_view_all);
                }
                String title = getString(R.string.hosts_at_location, hosts.size(), hosts.get(0).getLocation());

                tv.setText(Html.fromHtml(title));
                tv=(TextView)mPopup.findViewById(R.id.snippet);
                tv.setText(Html.fromHtml(hostList));

            }

            return(mPopup);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        Log.d(TAG, "onStop()");
        if (mLastCameraPosition != null) {
            saveMapLocation(mLastCameraPosition);
            Log.d(TAG, "Saved mLastCameraPosition");
        }
        super.onStop();
    }

    protected void saveMapLocation(CameraPosition position) {
        SharedPreferences settings = getSharedPreferences("map_last_location", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("latitude", (float)position.target.latitude);
        editor.putFloat("longitude", (float)position.target.longitude);
        editor.putFloat("zoom", (float) position.zoom);
        editor.commit();
    }


    /**
     * Retrieve map location and zoom from saved preference. Returns null if none existed.
     *
     * @return
     */
    protected CameraPosition getSavedCameraPosition() {
        SharedPreferences settings = getSharedPreferences("map_last_location", 0);
        if (!settings.contains("latitude")) {
            return null;
        }
        float latitude = settings.getFloat("latitude", Float.parseFloat(getResources().getString(R.string.map_default_latitude)));
        float longitude = settings.getFloat("longitude", Float.parseFloat(getResources().getString(R.string.map_default_longitude)));
        float zoom = settings.getFloat("zoom", (float)getResources().getInteger(R.integer.map_initial_zoom));

        CameraPosition position = new CameraPosition(new LatLng(latitude, longitude), zoom, 0, 0);
        return position;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(android.os.Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
    }

    /**
     * If we can get a location, go to it with default zoom.
     */
    void setMapToCurrentLocation() {
        LatLng gotoLatLng = new LatLng(mLastDeviceLocation.getLatitude(), mLastDeviceLocation.getLongitude());
        float zoom = (float) getResources().getInteger(R.integer.map_initial_zoom); // Default
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gotoLatLng, zoom));
    }

    @Override
    public void onCameraChange(CameraPosition position) {
        mLastCameraPosition = position;
        LatLngBounds curScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
        sendMessage(getResources().getString(R.string.loading_hosts), false);
        Search search = new RestMapSearch(curScreen.northeast, curScreen.southwest);
        doMapSearch(search);
    }

    public void doMapSearch(Search search) {
        searchTask = new MapSearchTask();
        searchTask.execute(search);
    }

    @Override
    /**
     * - Capture the clicked cluster so we can use it in custom infoWindow
     * - Check overall bounds of items in cluster
     * - If the bounds are empty (all hosts at same place) then let it pop the info window
     * - Otherwise, move the camera to show the bounds of the map
     */
    public boolean onClusterClick(Cluster<HostBriefInfo> cluster) {
        mLastClickedCluster = cluster; // remember for use later in the Adapter

        // Find out the bounds of the hosts currently in cluster
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(HostBriefInfo host : cluster.getItems()){
            builder.include(host.getLatLng());
        }
        LatLngBounds bounds = builder.build();

        // If the hosts are not all at the same location, then change bounds of map.
        if (!bounds.southwest.equals(bounds.northeast)) {
            // Offset from edge of map in pixels when exploding cluster
            View mapView = findViewById(R.id.map_fragment);
            int padding_percent = getResources().getInteger(R.integer.cluster_explode_padding_percent);
            int padding = Math.min(mapView.getHeight(), mapView.getWidth()) * padding_percent / 100;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, mapView.getWidth(), mapView.getHeight(), padding);
            mMap.animateCamera(cu);
            return true;
        }
        showMultihostSelectDialog((ArrayList<HostBriefInfo>)cluster.getItems());
        return true;
    }

    @Override
    /**
     * Start the Search tab with the members we have at this exact location.
     */
    public void onClusterInfoWindowClick(Cluster<HostBriefInfo> hostBriefInfoCluster) {
        Intent intent = new Intent(this, ListSearchTabActivity.class);
        intent.putParcelableArrayListExtra("search_results", (ArrayList<HostBriefInfo>) hostBriefInfoCluster.getItems());
        startActivity(intent);
    }

    @Override
    public boolean onClusterItemClick(HostBriefInfo hostBriefInfo) {
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(HostBriefInfo host) {
        Intent i = new Intent(this, HostInformationActivity.class);
        i.putExtra("host", Host.createFromBriefInfo(host));
        i.putExtra("id", host.getId());
        startActivityForResult(i, 0);
    }

    private class MapSearchTask extends AsyncTask<Search, Void, Object> {

        @Override
        protected Object doInBackground(Search... params) {
            Search search = params[0];
            Object retObj = null;

            try {
                retObj = search.doSearch();
            }
            catch (Exception e) {
                Log.e(WSAndroidApplication.TAG, e.getMessage(), e);
                retObj = e;
            }

            return retObj;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Exception) {
                // TODO: Test offline to see if this works
                if (result instanceof HttpException) {
                    Log.e(WSAndroidApplication.TAG, ((HttpException)(result)).getMessage());
                    sendMessage(getResources().getString(R.string.error_loading_hosts), true);
                }

                // TODO: Improve error reporting with more specifics
                sendMessage(getResources().getString(R.string.error_retrieving_host_information), true);
                return;
            }
            ArrayList<HostBriefInfo> hosts = (ArrayList<HostBriefInfo>) result;
            if (hosts.isEmpty()) {
                sendMessage((String)getResources().getText(R.string.no_results), false);
            }

            for (HostBriefInfo host: hosts) {
                HostBriefInfo v = mHosts.putIfAbsent(host.getId(), host);
                // Only add to the cluster if it wasn't in mHosts before.
                if (v == null) {
                    mClusterManager.addItem(host);
                }
            }
            mClusterManager.cluster();
        }

    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((Maps2Activity)getActivity()).onDialogDismissed();
        }
    }



    /*
     * Handle results returned to the FragmentActivity
     * by Google Play services
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONNECTION_FAILURE_RESOLUTION_REQUEST && resultCode == Activity.RESULT_OK) {
            mLocationClient.connect();
        }
    }

    /**
     * InfoWindowAdapter to present info about a single host marker.
     * Implemented here so we can have multiple lines, which the maps-provided one prevents.
     */
    class SingleHostInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private View mPopup = null;
        private LayoutInflater mInflater = null;

        SingleHostInfoWindowAdapter(LayoutInflater inflater) {
            this.mInflater = inflater;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return (null);
        }

        @SuppressLint("InflateParams")
        @Override
        public View getInfoContents(Marker marker) {
            if (mPopup == null){
                mPopup = mInflater.inflate(R.layout.single_host_infowindow, null);
            }
            TextView titleView = (TextView) mPopup.findViewById(R.id.title);
            titleView.setText(marker.getTitle());
            TextView snippetView = (TextView) mPopup.findViewById(R.id.snippet);
            snippetView.setText(Html.fromHtml(marker.getSnippet()));
            return (mPopup);
        }
    }

    public void showMultihostSelectDialog(final ArrayList<HostBriefInfo> hosts) {
        String[] mPossibleItems = new String[hosts.size()];

        double distance = Tools.calculateDistanceBetween(hosts.get(0).getLatLng(), mLastDeviceLocation, mDistanceUnit);
        String distanceSummary = getString(R.string.distance_from_current, (int) distance, mDistanceUnit);

        LinearLayout customTitleView = (LinearLayout)getLayoutInflater().inflate(R.layout.multihost_dialog_header, null);
        TextView titleView = (TextView)customTitleView.findViewById(R.id.title);
        titleView.setText(getString(R.string.hosts_at_location, hosts.size(), hosts.get(0).getStreetCityAddress()));

        TextView distanceView = (TextView)customTitleView.findViewById(R.id.distance_from_current);
        distanceView.setText(distanceSummary);

        for (int i = 0; i < hosts.size(); i++) {
            mPossibleItems[i] = hosts.get(i).getFullname();
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCustomTitle(customTitleView);

        alertDialogBuilder
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        }
                )
                .setItems(mPossibleItems,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int index) {
                                Intent intent = new Intent(Maps2Activity.this, HostInformationActivity.class);
                                HostBriefInfo briefHost = hosts.get(index);
                                Host host = Host.createFromBriefInfo(hosts.get(index));
                                intent.putExtra("host", host);
                                intent.putExtra("id", briefHost.getId());
                                startActivity(intent);
                            }

                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    private Toast lastToast = null;

    private void sendMessage(final String message, final boolean error) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        if (lastToast != null) {
            lastToast.cancel();
        }
        toast.show();
        lastToast = toast;
    }


}

