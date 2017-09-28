package fi.bitrite.android.ws.activity;

import android.annotation.SuppressLint;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.impl.RestMapSearch;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.WSNonHierarchicalDistanceBasedAlgorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class Maps2Activity extends WSBaseActivity implements
        ClusterManager.OnClusterClickListener<HostBriefInfo>,
        ClusterManager.OnClusterInfoWindowClickListener<HostBriefInfo>,
        ClusterManager.OnClusterItemClickListener<HostBriefInfo>,
        ClusterManager.OnClusterItemInfoWindowClickListener<HostBriefInfo>,
        GoogleMap.OnCameraChangeListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        SharedPreferences.OnSharedPreferenceChangeListener,
        OnMapReadyCallback {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private MapSearchTask searchTask;
    private ConcurrentHashMap<Integer, HostBriefInfo> mHosts = new ConcurrentHashMap<Integer, HostBriefInfo>();
    private ClusterManager<HostBriefInfo> mClusterManager;
    private Cluster<HostBriefInfo> mLastClickedCluster;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";

    private boolean mPlayServicesConnectionStatus = false;
    private static final String TAG = "Maps2Activity";
    private CameraPosition mLastCameraPosition = null;
    private boolean mResolvingError = false;
    Location mLastDeviceLocation;
    String mDistanceUnit;
    private boolean mIsOffline = false;
    StarredHostDao starredHostDao = new StarredHostDaoImpl();
    private List<HostBriefInfo> starredHosts;
    private GoogleApiClient mGoogleApiClient;
    enum ClusterStatus {none, some, all}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (!initView()) {
            finish();
            return;
        }

        // Google analytics tracker
        ((WSAndroidApplication) getApplication()).getTracker(WSAndroidApplication.TrackerName.APP_TRACKER);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        mDistanceUnit = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("distance_unit", "km");


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        setUpMapIfNeeded();
    }

    // Immediately handle change to distance unit if requrired
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("distance_unit")) {
            mDistanceUnit = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("distance_unit", "km");
        }
    }

    /**
     * This is where google play services gets connected and we can now find recent location.
     * <p/>
     * Note that all the complex stuff about connecting to Google Play Services (just to get location)
     * is from http://developer.android.com/training/location/retrieve-current.html and I don't actually
     * know how to test it.
     *
     * @param connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to Google Play services mLastCameraPosition==" + (mLastCameraPosition != null));

        mPlayServicesConnectionStatus = true;

        mLastDeviceLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        // If we are now connected, but still don't have a location, use a bogus default.
        if (mLastDeviceLocation == null) {
            mLastDeviceLocation = new Location("default");

            mLastDeviceLocation.setLatitude(
                    Double.parseDouble(getResources().getString(R.string.map_default_latitude)));
            mLastDeviceLocation.setLongitude(Double.parseDouble(getResources().getString(R.string.map_default_longitude)));
        }

        // mMap may not yet be initialized in some cases; Connect happens before map setup.
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);

            if (getSavedCameraPosition() == null) {
                setMapToCurrentLocation();
            }
        }
    }

    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Disconnected from play services");
        mPlayServicesConnectionStatus = false;
        Toast.makeText(this, getString(R.string.disconnected_location_services),
                Toast.LENGTH_SHORT).show();
    }

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
        private final IconGenerator mSingleLocationClusterIconGenerator = new IconGenerator(getApplicationContext());
        private final IconGenerator mSingleHostIconGenerator = new IconGenerator(getApplicationContext());
        private SparseArray<BitmapDescriptor> mIcons = new SparseArray<BitmapDescriptor>();
        private BitmapDescriptor mSingleHostBitmapDescriptor;

        public HostRenderer() {
            super(getApplicationContext(), mMap, mClusterManager);

            View sameLocationMultiHostClusterView = getLayoutInflater().inflate(R.layout.same_location_cluster_marker, null);
            View singleHostMarkerView = getLayoutInflater().inflate(R.layout.location_marker, null);
            mSingleLocationClusterIconGenerator.setContentView(sameLocationMultiHostClusterView);
            mSingleLocationClusterIconGenerator.setBackground(null);
            mSingleHostIconGenerator.setContentView(singleHostMarkerView);
            mSingleHostIconGenerator.setBackground(null);
            mSingleHostBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(mSingleHostIconGenerator.makeIcon());

        }

        @Override
        protected void onBeforeClusterRendered(Cluster<HostBriefInfo> cluster, MarkerOptions markerOptions) {

            if (clusterLocationStatus(cluster) == ClusterStatus.all) {
                int size = cluster.getSize();
                BitmapDescriptor descriptor = mIcons.get(size);
                if (descriptor == null) {
                    // Cache new bitmaps
                    descriptor = BitmapDescriptorFactory.fromBitmap(mSingleLocationClusterIconGenerator.makeIcon(String.valueOf(size)));
                    mIcons.put(size, descriptor);
                }
                markerOptions.icon(descriptor);
            } else {
                super.onBeforeClusterRendered(cluster, markerOptions);
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
                snippet += "<br/>" + getString(R.string.distance_from_current, (int) distance, mDistanceUnit);
            }
            markerOptions.title(host.getFullname()).snippet(snippet);
            markerOptions.icon(mSingleHostBitmapDescriptor);
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster<HostBriefInfo> cluster) {
            // Render as a cluster if all the items are at the exact same location, or if there are more than
            // min_cluster_size in the cluster.
            ClusterStatus status = clusterLocationStatus(cluster);
            boolean renderAsCluster = status == ClusterStatus.all || status == ClusterStatus.some || cluster.getSize() >= getResources().getInteger(R.integer.min_cluster_size);
            return renderAsCluster;
        }

        /**
         * Attempt to determine the location status of items in the cluster, whether all in one location
         * or in a variety of locations.
         *
         * @param cluster
         * @return
         */
        protected ClusterStatus clusterLocationStatus(Cluster<HostBriefInfo> cluster) {

            HashSet<String> latLngs = new HashSet<String>();
            for (HostBriefInfo item : cluster.getItems()) {
                latLngs.add(item.getLatLng().toString());
            }

            // if cluster size and latLngs size are same, all are unique locations, so 'none'
            if (cluster.getSize() == latLngs.size()) {
                return ClusterStatus.none;
            }
            // If there is only one unique location, then all are in same location.
            else if (latLngs.size() == 1) {
                return ClusterStatus.all;
            }
            // Otherwise it's a mix of same and other location
            return ClusterStatus.some;
        }
    }


    class ClusterInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private View mPopup = null;
        private LayoutInflater mInflater = null;

        ClusterInfoWindowAdapter(LayoutInflater inflater) {
            this.mInflater = inflater;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {

            StringBuilder hostList = new StringBuilder();
            ArrayList<HostBriefInfo> hosts = new ArrayList<HostBriefInfo>();
            if (mPopup == null) {
                mPopup = mInflater.inflate(R.layout.info_window, null);
            }
            TextView tv = (TextView) mPopup.findViewById(R.id.title);

            if (mLastClickedCluster != null) {

                if (mLastDeviceLocation != null) {
                    double distance = Tools.calculateDistanceBetween(marker.getPosition(), mLastDeviceLocation, mDistanceUnit);
                    TextView distance_tv = (TextView) mPopup.findViewById(R.id.distance_from_current);
                    distance_tv.setText(Html.fromHtml(getString(R.string.distance_from_current, (int) distance, mDistanceUnit)));
                }

                hosts = (ArrayList<HostBriefInfo>) mLastClickedCluster.getItems();
                if (mLastClickedCluster != null) {
                    for (HostBriefInfo host : hosts) {
                        hostList.append(host.getFullname()).append("<br/>");
                    }

                    hostList.append(getString(R.string.click_to_view_all));
                }
                String title = getResources().getQuantityString(R.plurals.hosts_at_location, hosts.size(), hosts.size(), hosts.get(0).getLocation());

                tv.setText(Html.fromHtml(title));
                tv = (TextView) mPopup.findViewById(R.id.snippet);
                tv.setText(Html.fromHtml(hostList.toString()));

            }

            return (mPopup);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We'll use the starred hosts when network is offline.
        starredHostDao.open();
        starredHosts = starredHostDao.getAllBrief();

        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        starredHostDao.close();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mLastCameraPosition != null) {
            saveMapLocation(mLastCameraPosition);
        }
        GoogleAnalytics.getInstance(this).reportActivityStop(this);

        super.onStop();
    }

    protected void saveMapLocation(CameraPosition position) {
        SharedPreferences settings = getSharedPreferences("map_last_location", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("latitude", (float) position.target.latitude);
        editor.putFloat("longitude", (float) position.target.longitude);
        editor.putFloat("zoom", (float) position.zoom);
        editor.apply();
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
        float zoom = settings.getFloat("zoom", (float) getResources().getInteger(R.integer.map_initial_zoom));

        CameraPosition position = new CameraPosition(new LatLng(latitude, longitude), zoom, 0, 0);
        return position;
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(android.os.Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map_fragment);
            mapFragment.getMapAsync(this);
        }
    }

    public void onMapReady(GoogleMap map) {
        mMap = map;
        setUpMap();
        mGoogleApiClient.connect(); // Can't connect until here because location will need map to act
    }

    private void setUpMap() {

        // Rotate gestures probably aren't needed here and can be disorienting for some of our users.
        mMap.getUiSettings().setRotateGesturesEnabled(false);

        mMap.setOnCameraChangeListener(this);

        CameraPosition position = null;

        // If we were launched with an intent asking us to zoom to a member
        Intent receivedIntent = getIntent();
        if (receivedIntent.hasExtra("target_map_latlng")) {
            LatLng targetLatLng = receivedIntent.getParcelableExtra("target_map_latlng");
            position = new CameraPosition(targetLatLng, getResources().getInteger(R.integer.map_showhost_zoom), 0, 0);
        }

        if (position == null) {
            position = getSavedCameraPosition();
        }
        if (position != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
            // The move itself will end up setting the mlastCameraPosition.
        }

        mClusterManager = new ClusterManager<HostBriefInfo>(this, mMap);
        mClusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<HostBriefInfo>(new WSNonHierarchicalDistanceBasedAlgorithm<HostBriefInfo>(this)));
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

        // If not connected, we'll switch to offline/starred hosts mode
        if (!Tools.isNetworkConnected(this)) {
            sendMessage(R.string.map_network_not_connected, false);
            // If we already knew we were offline, return
            if (mIsOffline) {
                return;
            }
            // Otherwise, set state to offline and load only offline hosts
            mIsOffline = true;
            loadOfflineHosts();
            return;
        }

        // If we were offline, switch back on, but remove the offline markers
        if (mIsOffline) {
            mIsOffline = false;
            mClusterManager.clearItems();
            mClusterManager.getMarkerCollection().clear();
            mHosts.clear();
        }

        // And get standard host list for region from server
        LatLngBounds curScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
        Search search = new RestMapSearch(curScreen.northeast, curScreen.southwest);

        if (position.zoom < getResources().getInteger(R.integer.map_zoom_min_load)) {
            sendMessage(R.string.hosts_dont_load, false);
        } else {
            sendMessage(getResources().getString(R.string.loading_hosts), false);
            doMapSearch(search);
        }
    }

    private void loadOfflineHosts() {
        mClusterManager.clearItems();
        mClusterManager.getMarkerCollection().clear();
        mHosts.clear();
        mClusterManager.addItems(starredHosts);
        mClusterManager.cluster();
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
        for (HostBriefInfo host : cluster.getItems()) {
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
        showMultihostSelectDialog((ArrayList<HostBriefInfo>) cluster.getItems());
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
        private static final String TAG = "MapSearchTask";

        @Override
        protected Object doInBackground(Search... params) {
            Search search = params[0];
            Object retObj = null;

            try {
                retObj = search.doSearch();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                retObj = e;
            }

            return retObj;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Exception) {
                RestClient.reportError(Maps2Activity.this, result);
                return;
            }

            ArrayList<HostBriefInfo> hosts = (ArrayList<HostBriefInfo>) result;
            if (hosts.isEmpty()) {
                sendMessage((String) getResources().getText(R.string.no_results), false);
            }

            for (HostBriefInfo host : hosts) {
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
        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((Maps2Activity) getActivity()).onDialogDismissed();
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
            if (mPopup == null) {
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

        LinearLayout customTitleView = (LinearLayout) getLayoutInflater().inflate(R.layout.multihost_dialog_header, null);
        TextView titleView = (TextView) customTitleView.findViewById(R.id.title);
        titleView.setText(getResources().getQuantityString(R.plurals.hosts_at_location, hosts.size(), hosts.size(), hosts.get(0).getStreetCityAddress()));

        TextView distanceView = (TextView) customTitleView.findViewById(R.id.distance_from_current);
        distanceView.setText(distanceSummary);

        for (int i = 0; i < hosts.size(); i++) {
            mPossibleItems[i] = hosts.get(i).getFullname();
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCustomTitle(customTitleView);

        alertDialogBuilder
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
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

    private void sendMessage(int message_id, final boolean error) {
        String message = getString(message_id);
        sendMessage(message, error);
    }

    private void sendMessage(final String message, final boolean error) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        if (lastToast != null) {
            lastToast.cancel();
        }
        toast.show();
        lastToast = toast;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_actions, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) menu.findItem(R.id.action_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.legal) {
            startActivity(new Intent(this, LegalNoticesActivity.class));

            return(true);
        }

        return super.onOptionsItemSelected(item);
    }

}

