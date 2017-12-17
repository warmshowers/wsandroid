package fi.bitrite.android.ws.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.impl.RestMapSearch;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;
import fi.bitrite.android.ws.ui.util.NavigationController;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.WSNonHierarchicalDistanceBasedAlgorithm;
import io.reactivex.subjects.CompletableSubject;

public class MapFragment extends BaseFragment implements
        ClusterManager.OnClusterClickListener<HostBriefInfo>,
        ClusterManager.OnClusterInfoWindowClickListener<HostBriefInfo>,
        ClusterManager.OnClusterItemClickListener<HostBriefInfo>,
        ClusterManager.OnClusterItemInfoWindowClickListener<HostBriefInfo>,
        GoogleMap.OnCameraChangeListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final String KEY_MAP_TARGET_LAT_LNG = "map_target_lat_lng";

    @Inject NavigationController mNavigationController;
    @Inject AuthenticationController mAuthenticationController;

    private Unbinder mUnbinder;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private MapSearchTask searchTask;
    private ConcurrentHashMap<Integer, HostBriefInfo> mHosts = new ConcurrentHashMap<Integer, HostBriefInfo>();
    private ClusterManager<HostBriefInfo> mClusterManager;
    private Cluster<HostBriefInfo> mLastClickedCluster;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";

    private static final String TAG = "MapFragment";
    private CameraPosition mLastCameraPosition = null;
    private boolean mResolvingError = false;
    Location mLastDeviceLocation;
    String mDistanceUnit;
    private boolean mIsOffline = false;
    StarredHostDao starredHostDao = new StarredHostDaoImpl();
    private List<HostBriefInfo> starredHosts;
    private GoogleApiClient mGoogleApiClient;
    enum ClusterStatus {none, some, all}

    public static MapFragment create() {
        MapFragment mapFragment = new MapFragment();
        Bundle bundle = new Bundle();
        mapFragment.setArguments(bundle);
        return mapFragment;
    }
    public static MapFragment create(LatLng latLng) {
        MapFragment mapFragment = create();
        mapFragment.getArguments().putParcelable(KEY_MAP_TARGET_LAT_LNG, latLng);
        return mapFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        mDistanceUnit = sharedPreferences.getString("distance_unit", "km");
        sharedPreferences.registerOnSharedPreferenceChangeListener((unused, key) -> {
            if ("distance_unit".equals(key)) {
                mDistanceUnit = sharedPreferences.getString("distance_unit", "km");
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mUnbinder = ButterKnife.bind(this, view);

        setUpMapIfNeeded();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // We'll use the starred hosts when network is offline.
        starredHostDao.open();
        starredHosts = starredHostDao.getAllBrief();

        setUpMapIfNeeded();
    }

    @Override
    public void onPause() {
        starredHostDao.close();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mLastCameraPosition != null) {
            saveMapLocation(mLastCameraPosition);
        }

        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(android.os.Bundle)} may not be called again so we should
     * call this method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            SupportMapFragment supportMapFragment =
                    (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            supportMapFragment.getMapAsync(map -> {
                mMap = map;
                setUpMap();

                // Can't connect until here because location will need map to act
                mGoogleApiClient.connect();
            });
        }
    }

    private void setUpMap() {
        // Rotate gestures probably aren't needed here and can be disorienting for some of our users.
        mMap.getUiSettings().setRotateGesturesEnabled(false);

        // Adds a button to navigate to the current GPS position.
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            mMap.setMyLocationEnabled(true); // Requires ACCESS_FINE_LOCATION
        } else {
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 0);
        }

        mMap.setOnCameraChangeListener(this);

        // If we were launched with an intent asking us to zoom to a member
        LatLng targetLatLng = getArguments().getParcelable(KEY_MAP_TARGET_LAT_LNG);

        CameraPosition position;
        if (targetLatLng != null) {
            float showHostZoom = getResources().getInteger(R.integer.map_showhost_zoom);
            position = new CameraPosition(targetLatLng, showHostZoom, 0, 0);
        } else {
            // Fetches the last location from the settings.
            position = getSavedCameraPosition();
        }
        if (position != null) {
            // The move will end up setting mLastCameraPosition.
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }

        mClusterManager = new ClusterManager<>(getContext(), mMap);
        mClusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<>(
                new WSNonHierarchicalDistanceBasedAlgorithm<>(getContext())));
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

    private boolean hasPermission(String permission) {
        return PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(getContext(), permission);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // ACCESS_FINE_LOCATION is granted.
            if (mMap != null) {
                mMap.setMyLocationEnabled(true); // Requires ACCESS_FINE_LOCATION
            }
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

        mLastDeviceLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        // If we are now connected, but still don't have a location, use a bogus default.
        if (mLastDeviceLocation == null) {
            mLastDeviceLocation = new Location("default");

            mLastDeviceLocation.setLatitude(Double.parseDouble(getString(R.string.map_default_latitude)));
            mLastDeviceLocation.setLongitude(Double.parseDouble(getString(R.string.map_default_longitude)));
        }

        // mMap may not yet be initialized in some cases; Connect happens before map setup.
        if (mMap != null) {
            //mMap.setMyLocationEnabled(true);

            if (getSavedCameraPosition() == null) {
                setMapToCurrentLocation();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Disconnected from play services");

        Toast.makeText(getContext(), getString(R.string.disconnected_location_services),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        getActivity(), CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 */
            } catch (IntentSender.SendIntentException e) {
                // Thrown if Google Play services canceled the original PendingIntent.

                // Log the error
                e.printStackTrace();
            }
        } else {
            // If no resolution is onSuccess, display a dialog to the  user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    /**
     * Add the title and snippet to the marker so that infoWindow can be rendered.
     */
    private class HostRenderer extends DefaultClusterRenderer<HostBriefInfo> {
        private final IconGenerator mSingleLocationClusterIconGenerator = new IconGenerator(getActivity().getApplicationContext());
        private final IconGenerator mSingleHostIconGenerator = new IconGenerator(getActivity().getApplicationContext());
        private SparseArray<BitmapDescriptor> mIcons = new SparseArray<BitmapDescriptor>();
        private BitmapDescriptor mSingleHostBitmapDescriptor;

        public HostRenderer() {
            super(getActivity().getApplicationContext(), mMap, mClusterManager);

            View sameLocationMultiHostClusterView = getLayoutInflater().inflate(R.layout.marker_location_cluster, null);
            View singleHostMarkerView = getLayoutInflater().inflate(R.layout.marker_location, null);
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
                mPopup = mInflater.inflate(R.layout.view_user_info_multiple, null);
            }
            TextView tv = (TextView) mPopup.findViewById(R.id.title);

            if (mLastClickedCluster != null) {

                if (mLastDeviceLocation != null) {
                    double distance = Tools.calculateDistanceBetween(marker.getPosition(), mLastDeviceLocation, mDistanceUnit);
                    TextView distance_tv = (TextView) mPopup.findViewById(R.id.distance_from_current);
                    distance_tv.setText(Html.fromHtml(getString(R.string.distance_from_current, (int) distance, mDistanceUnit)));
                }

                hosts = (ArrayList<HostBriefInfo>) mLastClickedCluster.getItems();
                Collections.sort(hosts, (left, right) -> {
                    // TODO(saemy): Unify with the algorithm used in {@link SearchFragment}
                    int ncaLeft = left.getNotCurrentlyAvailableAsInt();
                    int ncaRight = right.getNotCurrentlyAvailableAsInt();

                    return ncaLeft != ncaRight
                            ? ncaLeft - ncaRight
                            : left.getFullname().compareTo(right.getFullname());

                });

                for (HostBriefInfo host : hosts) {
                    hostList.append(host.getFullname()).append("<br/>");
                }
                hostList.append(getString(R.string.click_to_view_all));

                String title = getResources().getQuantityString(R.plurals.hosts_at_location, hosts.size(), hosts.size(), hosts.get(0).getLocation());

                tv.setText(Html.fromHtml(title));
                tv = (TextView) mPopup.findViewById(R.id.snippet);
                tv.setText(Html.fromHtml(hostList.toString()));
            }

            return (mPopup);
        }
    }

    protected void saveMapLocation(CameraPosition position) {
        SharedPreferences settings = getActivity().getSharedPreferences("map_last_location", 0);
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
        SharedPreferences settings = getActivity().getSharedPreferences("map_last_location", 0);
        if (!settings.contains("latitude")) {
            return null;
        }
        float latitude = settings.getFloat("latitude", Float.parseFloat(getResources().getString(R.string.map_default_latitude)));
        float longitude = settings.getFloat("longitude", Float.parseFloat(getResources().getString(R.string.map_default_longitude)));
        float zoom = settings.getFloat("zoom", (float) getResources().getInteger(R.integer.map_initial_zoom));

        CameraPosition position = new CameraPosition(new LatLng(latitude, longitude), zoom, 0, 0);
        return position;
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
        if (!Tools.isNetworkConnected(getContext())) {
            sendMessage(R.string.map_network_not_connected);
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
        Search search = new RestMapSearch(
                mAuthenticationController, curScreen.northeast, curScreen.southwest);

        if (position.zoom < getResources().getInteger(R.integer.map_zoom_min_load)) {
            sendMessage(R.string.hosts_dont_load);
        } else {
            sendMessage(R.string.loading_hosts);
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

    /**
     * - Capture the clicked cluster so we can use it in custom infoWindow
     * - Check overall bounds of items in cluster
     * - If the bounds are empty (all hosts at same place) then let it pop the info window
     * - Otherwise, move the camera to show the bounds of the map
     */
    @Override
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
            View mapView = getChildFragmentManager().findFragmentById(R.id.map).getView();
            int padding_percent = getResources().getInteger(R.integer.cluster_explode_padding_percent);
            int padding = Math.min(mapView.getHeight(), mapView.getWidth()) * padding_percent / 100;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, mapView.getWidth(), mapView.getHeight(), padding);
            mMap.animateCamera(cu);
            return true;
        }
        showMultihostSelectDialog((ArrayList<HostBriefInfo>) cluster.getItems());
        return true;
    }

    /**
     * Start the Search tab with the members we have at this exact location.
     */
    @Override
    public void onClusterInfoWindowClick(Cluster<HostBriefInfo> hostBriefInfoCluster) {
        ArrayList<HostBriefInfo> users = (ArrayList<HostBriefInfo>) hostBriefInfoCluster.getItems();
        mNavigationController.navigateToUserList(users);
    }

    @Override
    public boolean onClusterItemClick(HostBriefInfo hostBriefInfo) {
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(HostBriefInfo user) {
        mNavigationController.navigateToUser(user.getId());
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
                RestClient.reportError(getContext(), result);
                return;
            }

            ArrayList<HostBriefInfo> hosts = (ArrayList<HostBriefInfo>) result;
            if (hosts.isEmpty()) {
                sendMessage(R.string.no_results);
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
        ErrorDialogFragment dialogFragment = ErrorDialogFragment.create(errorCode);
        dialogFragment.getCompletable().subscribe(() -> mResolvingError = false);
        dialogFragment.show(getChildFragmentManager(), "errordialog");
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        private final CompletableSubject mCompletable = CompletableSubject.create();

        public static ErrorDialogFragment create(int errorCode) {
            Bundle args = new Bundle();
            args.putInt(DIALOG_ERROR, errorCode);

            ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(
                    errorCode, getActivity(), REQUEST_RESOLVE_ERROR);
        }

        public CompletableSubject getCompletable() {
            return mCompletable;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            mCompletable.onComplete();
        }
    }



    /**
     * InfoWindowAdapter to present info about a single host marker.
     * Implemented here so we can have multiple lines, which the maps-provided one prevents.
     */
    class SingleHostInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        @BindView(R.id.title) TextView mLblTitle;
        @BindView(R.id.snippet) TextView mLblSnippet;

        private View mPopup = null;
        private LayoutInflater mInflater = null;

        SingleHostInfoWindowAdapter(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getInfoContents(Marker marker) {
            if (mPopup == null) {
                mPopup = mInflater.inflate(R.layout.view_user_info_single, null);
            }
            ButterKnife.bind(this, mPopup);

            mLblTitle.setText(marker.getTitle());
            mLblSnippet.setText(Html.fromHtml(marker.getSnippet()));

            return mPopup;
        }
    }

    public void showMultihostSelectDialog(final ArrayList<HostBriefInfo> users) {
        String[] mPossibleItems = new String[users.size()];

        double distance = Tools.calculateDistanceBetween(users.get(0).getLatLng(), mLastDeviceLocation, mDistanceUnit);
        String distanceSummary = getString(R.string.distance_from_current, (int) distance, mDistanceUnit);

        LinearLayout customTitleView = (LinearLayout) getLayoutInflater().inflate(R.layout.view_multiuser_dialog_header, null);
        TextView titleView = (TextView) customTitleView.findViewById(R.id.title);
        titleView.setText(getResources().getQuantityString(R.plurals.hosts_at_location, users.size(), users.size(), users.get(0).getStreetCityAddress()));

        TextView distanceView = (TextView) customTitleView.findViewById(R.id.distance_from_current);
        distanceView.setText(distanceSummary);

        for (int i = 0; i < users.size(); i++) {
            mPossibleItems[i] = users.get(i).getFullname();
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setCustomTitle(customTitleView);

        alertDialogBuilder
                .setNegativeButton(R.string.ok, (dialog, which) -> {})
                .setItems(mPossibleItems, (dialog, index) -> {
                    HostBriefInfo briefHost = users.get(index);
                    mNavigationController.navigateToUser(briefHost.getId());
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    private Toast mLastToast = null;
    private void sendMessage(@StringRes final int messageId) {
        if (mLastToast != null) {
            mLastToast.cancel();
        }

        mLastToast = Toast.makeText(getContext(), messageId, Toast.LENGTH_SHORT);
        mLastToast.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map_actions, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager =
                (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.app_title);
    }
}

