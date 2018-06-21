package fi.bitrite.android.ws.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.TextUtils;
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

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import java.util.concurrent.ConcurrentSkipListSet;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.api.response.UserSearchByLocationResponse;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.FavoriteRepository;
import fi.bitrite.android.ws.repository.SettingsRepository;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.model.ClusterUser;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.WSNonHierarchicalDistanceBasedAlgorithm;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.CompletableSubject;

public class MapFragment extends BaseFragment implements
        ClusterManager.OnClusterClickListener<ClusterUser>,
        ClusterManager.OnClusterInfoWindowClickListener<ClusterUser>,
        ClusterManager.OnClusterItemClickListener<ClusterUser>,
        ClusterManager.OnClusterItemInfoWindowClickListener<ClusterUser>,
        GoogleMap.OnCameraChangeListener {

    private static final String KEY_MAP_TARGET_LAT_LNG = "map_target_lat_lng";
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String TAG = "MapFragment";

    @Inject LoggedInUserHelper mLoggedInUserHelper;
    @Inject UserRepository mUserRepository;
    @Inject FavoriteRepository mFavoriteRepository;
    @Inject SettingsRepository mSettingsRepository;

    private Unbinder mUnbinder;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private ConcurrentSkipListSet<Integer> mClusteredUsers = new ConcurrentSkipListSet<>();
    private ClusterManager<ClusterUser> mClusterManager;
    private Cluster<ClusterUser> mLastClickedCluster;

    private boolean mIsOffline = false;

    private Disposable mLoadOfflineUserDisposable;
    private Toast mLastToast = null;

    private SettingsRepository.DistanceUnit mDistanceUnit;
    private String mDistanceUnitShort;
    private final SharedPreferences.OnSharedPreferenceChangeListener mOnSettingsChangeListener =
            (unused, key) -> {
                if (key == null || key.equals(mSettingsRepository.getDistanceUnitKey())) {
                    mDistanceUnit = mSettingsRepository.getDistanceUnit();
                    mDistanceUnitShort = mSettingsRepository.getDistanceUnitShort();
                }
            };

    private static final int POSITION_PRIORITY_ESTIMATE = 0;
    private static final int POSITION_PRIORITY_LAST_DEVICE_POSITION = 1;
    private static final int POSITION_PRIORITY_LAST_STORED = 2;
    private static final int POSITION_PRIORITY_FORCED = 100;

    private int mLastPositionType = -1;
    private CameraPosition mLastCameraPosition = null;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLastDeviceLocation;
    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }

            mLastDeviceLocation = locationResult.getLastLocation();
            // FIXME(saemy): Clear cluster info window cache as the distance to this distance is not
            //               re-calculated.

            // As we know more location details, we do (another) initial map move. This does not
            // affect the current location, if we already moved to a more detailed location.
            doInitialMapMove();
        }
    };

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

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
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

        // Register the settings change listener. That does an initial call to the handler.
        mSettingsRepository.registerOnChangeListener(mOnSettingsChangeListener);

        LocationRequest locationRequest = new LocationRequest()
                .setInterval(60000)
                .setFastestInterval(60000)
                .setPriority(LocationRequest.PRIORITY_LOW_POWER);
        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);

        setUpMapIfNeeded();
    }

    @Override
    public void onPause() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        mSettingsRepository.unregisterOnChangeListener(mOnSettingsChangeListener);

        super.onPause();
    }

    @Override
    public void onStop() {
        if (mLastCameraPosition != null) {
            mSettingsRepository.setLastMapLocation(mLastCameraPosition);
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
            requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, 0);
        }

        mMap.setOnCameraChangeListener(this);

        doInitialMapMove();

        mClusterManager = new ClusterManager<>(getContext(), mMap);
        mClusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<>(
                new WSNonHierarchicalDistanceBasedAlgorithm<>(getContext())));
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);
        mClusterManager.setRenderer(new UserRenderer());
        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());
        mClusterManager.getClusterMarkerCollection()
                .setOnInfoWindowAdapter(new ClusterInfoWindowAdapter(getLayoutInflater()));
        mClusterManager.getMarkerCollection()
                .setOnInfoWindowAdapter(new SingleUserInfoWindowAdapter(getLayoutInflater()));
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
     * Moves the map to the given location if the given priority is newer than the current one.
     * A default value for zoom is used if the given value is less than zero.
     */
    private void moveMapToLocation(LatLng latLng, float zoom, int positionPriority) {
        if (mMap == null || latLng == null) {
            return;
        }

        if (mLastPositionType < positionPriority) {
            mLastPositionType = positionPriority;

            if (zoom < 0) {
                zoom = getResources().getInteger(R.integer.prefs_map_location_zoom_default);
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        }
    }
    private void doInitialMapMove() {
        float showUserZoom = getResources().getInteger(R.integer.map_showuser_zoom);

        // If we were launched with an intent asking us to zoom to a member
        LatLng targetLatLng = getArguments().getParcelable(KEY_MAP_TARGET_LAT_LNG);
        if (targetLatLng != null) {
            moveMapToLocation(targetLatLng, showUserZoom, POSITION_PRIORITY_FORCED);
            return;
        }

        // Fetches the last location from the settings (but no default yet).
        CameraPosition savedLocation = mSettingsRepository.getLastMapLocation(false);
        if (savedLocation != null) {
            moveMapToLocation(
                    savedLocation.target, savedLocation.zoom, POSITION_PRIORITY_LAST_STORED);
            return;
        }

        if (mLastDeviceLocation != null) {
            moveMapToLocation(Tools.locationToLatLng(mLastDeviceLocation), showUserZoom,
                    POSITION_PRIORITY_LAST_DEVICE_POSITION);
            return;
        }

        // If we are now connected, but still don't have a location, use a bogus default.
        User loggedInUser = mLoggedInUserHelper.get();
        if (loggedInUser != null && loggedInUser.location != null) {
            moveMapToLocation(loggedInUser.location, showUserZoom, POSITION_PRIORITY_ESTIMATE);
            return;
        }

        // Fetches the default last location from the settings.
        savedLocation = mSettingsRepository.getLastMapLocation(true);
        moveMapToLocation(savedLocation.target, savedLocation.zoom, POSITION_PRIORITY_ESTIMATE);
    }

    @Override
    public void onCameraChange(CameraPosition position) {
        mLastCameraPosition = position;

        // If not connected, we'll switch to offline/starred user mode
        if (!Tools.isNetworkConnected(getContext())) {
            sendMessage(R.string.map_network_not_connected);
            // If we already knew we were offline, return
            if (mIsOffline) {
                return;
            }
            // Otherwise, set state to offline and load only offline user
            mIsOffline = true;
            loadOfflineUsers();
            return;
        }

        // If we were offline, switch back on, but remove the offline markers
        if (mIsOffline) {
            mIsOffline = false;

            // Stop listening for the favorite users.
            if (mLoadOfflineUserDisposable != null) {
                mLoadOfflineUserDisposable.dispose();
                mLoadOfflineUserDisposable = null;
            }

            mClusterManager.clearItems();
            mClusterManager.getMarkerCollection().clear();
            mClusteredUsers.clear();
        }

        // And get standard user list for region from server
        if (position.zoom < getResources().getInteger(R.integer.map_zoom_min_load)) {
            sendMessage(R.string.users_dont_load);
        } else {
            sendMessage(R.string.loading_users);

            LatLngBounds curScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
            getResumePauseDisposable().add(
                    mUserRepository.searchByLocation(curScreen.northeast, curScreen.southwest)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(searchResult -> {
                                if (searchResult.isEmpty()) {
                                    sendMessage(R.string.no_results);
                                }

                                for (UserSearchByLocationResponse.User user : searchResult) {
                                    boolean isNew = mClusteredUsers.add(user.id);
                                    // Only add to the cluster if it wasn't before.
                                    if (isNew) {
                                        mClusterManager.addItem(ClusterUser.from(user));
                                    }
                                }
                                mClusterManager.cluster();
                            }, throwable -> {
                                // TODO(saemy): Error handling.
                                Log.e(TAG, throwable.getMessage());
                            }));
        }
    }

    private void loadOfflineUsers() {
        mClusterManager.clearItems();
        mClusterManager.getMarkerCollection().clear();
        mClusteredUsers.clear();

        // We'll use the starred users when network is offline.
        List<Integer> loadedUserIds = new ArrayList<>();
        mLoadOfflineUserDisposable = Observable.merge(mFavoriteRepository.getFavorites())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userResource -> {
                    if (userResource.hasData() && mIsOffline) {
                        // Users pop up twice as one is the error since we cannot load it from the
                        // network.
                        User user = userResource.data;
                        if (loadedUserIds.contains(user.id)) {
                            return;
                        }
                        loadedUserIds.add(user.id);

                        mClusterManager.addItem(ClusterUser.from(user));
                        mClusterManager.cluster();
                    }
                });
        getResumePauseDisposable().add(mLoadOfflineUserDisposable);
    }

    /**
     * - Capture the clicked cluster so we can use it in custom infoWindow
     * - Check overall bounds of items in cluster
     * - If the bounds are empty (all users at same place) then let it pop the info window
     * - Otherwise, move the camera to show the bounds of the map
     */
    @Override
    public boolean onClusterClick(Cluster<ClusterUser> cluster) {
        mLastClickedCluster = cluster; // remember for use later in the Adapter

        // Find out the bounds of the users currently in cluster
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (ClusterUser user : cluster.getItems()) {
            builder.include(user.latLng);
        }
        LatLngBounds bounds = builder.build();

        // If the users are not all at the same location, then change bounds of map.
        if (!bounds.southwest.equals(bounds.northeast)) {
            // Offset from edge of map in pixels when exploding cluster
            View mapView = getChildFragmentManager().findFragmentById(R.id.map).getView();
            int padding_percent =
                    getResources().getInteger(R.integer.cluster_explode_padding_percent);
            int padding = Math.min(mapView.getHeight(), mapView.getWidth()) * padding_percent / 100;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, mapView.getWidth(),
                    mapView.getHeight(), padding);
            mMap.animateCamera(cu);
            return true;
        }
        showMultiUserSelectDialog((ArrayList<ClusterUser>) cluster.getItems());
        return true;
    }

    /**
     * Start the Search tab with the members we have at this exact location.
     */
    @Override
    public void onClusterInfoWindowClick(Cluster<ClusterUser> cluster) {
        ArrayList<Integer> userIds = new ArrayList<>(cluster.getSize());
        for (ClusterUser user : cluster.getItems()) {
            userIds.add(user.id);
        }
        getNavigationController().navigateToUserList(userIds);
    }

    @Override
    public boolean onClusterItemClick(ClusterUser user) {
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(ClusterUser user) {
        getNavigationController().navigateToUser(user.id);
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        ErrorDialogFragment dialogFragment = ErrorDialogFragment.create(errorCode);
        dialogFragment.show(getChildFragmentManager(), "errordialog");
    }

    /**
     * Returns the distance to given point or -1 if the current position is unknown.
     */
    private double calculateDistanceTo(LatLng latLng) {
        return mLastDeviceLocation != null
                ? Tools.calculateDistanceBetween(
                Tools.latLngToLocation(latLng), mLastDeviceLocation, mDistanceUnit)
                : -1;
    }

    public void showMultiUserSelectDialog(final ArrayList<ClusterUser> users) {
        String[] mPossibleItems = new String[users.size()];

        double distance = calculateDistanceTo(users.get(0).latLng);
        String distanceSummary =
                getString(R.string.distance_from_current, (int) distance, mDistanceUnitShort);

        LinearLayout customTitleView = (LinearLayout) getLayoutInflater().inflate(
                R.layout.view_multiuser_dialog_header, null);
        TextView titleView = customTitleView.findViewById(R.id.title);
        titleView.setText(getResources().getQuantityString(R.plurals.users_at_location,
                users.size(), users.size(), users.get(0).getStreetCityAddressStr()));

        TextView distanceView = customTitleView.findViewById(R.id.distance_from_current);
        distanceView.setText(distance >= 0 ? distanceSummary : "");

        for (int i = 0; i < users.size(); i++) {
            mPossibleItems[i] = users.get(i).fullname;
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setCustomTitle(customTitleView);

        alertDialogBuilder
                .setNegativeButton(R.string.ok, (dialog, which) -> {
                })
                .setItems(mPossibleItems, (dialog, index) -> {
                    ClusterUser user = users.get(index);
                    getNavigationController().navigateToUser(user.id);
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

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
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.app_title);
    }

    enum ClusterStatus {none, some, all}

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
     * Add the title and snippet to the marker so that infoWindow can be rendered.
     */
    private class UserRenderer extends DefaultClusterRenderer<ClusterUser> {
        private final IconGenerator mSingleLocationClusterIconGenerator =
                new IconGenerator(getActivity().getApplicationContext());
        private final IconGenerator mSingleUserIconGenerator =
                new IconGenerator(getActivity().getApplicationContext());
        private SparseArray<BitmapDescriptor> mIcons = new SparseArray<>();
        private BitmapDescriptor mSingleUserBitmapDescriptor;

        public UserRenderer() {
            super(getActivity().getApplicationContext(), mMap, mClusterManager);

            View sameLocationMultiUserClusterView =
                    getLayoutInflater().inflate(R.layout.marker_location_cluster, null);
            View singleUserMarkerView = getLayoutInflater().inflate(R.layout.marker_location, null);
            mSingleLocationClusterIconGenerator.setContentView(sameLocationMultiUserClusterView);
            mSingleLocationClusterIconGenerator.setBackground(null);
            mSingleUserIconGenerator.setContentView(singleUserMarkerView);
            mSingleUserIconGenerator.setBackground(null);
            mSingleUserBitmapDescriptor =
                    BitmapDescriptorFactory.fromBitmap(mSingleUserIconGenerator.makeIcon());

        }

        @Override
        protected void onBeforeClusterRendered(Cluster<ClusterUser> cluster,
                                               MarkerOptions markerOptions) {

            if (clusterLocationStatus(cluster) == ClusterStatus.all) {
                int size = cluster.getSize();
                BitmapDescriptor descriptor = mIcons.get(size);
                if (descriptor == null) {
                    // Cache new bitmaps
                    descriptor = BitmapDescriptorFactory.fromBitmap(
                            mSingleLocationClusterIconGenerator.makeIcon(String.valueOf(size)));
                    mIcons.put(size, descriptor);
                }
                markerOptions.icon(descriptor);
            } else {
                super.onBeforeClusterRendered(cluster, markerOptions);
            }
        }

        @Override
        protected void onBeforeClusterItemRendered(ClusterUser user, MarkerOptions markerOptions) {
            StringBuilder snippet = new StringBuilder();
            if (!TextUtils.isEmpty(user.street)) {
                snippet.append(user.street).append("<br/>");
            }
            snippet.append(user.city).append(", ").append(user.province.toUpperCase());

            double distance = calculateDistanceTo(user.latLng);
            if (distance >= 0) {
                snippet.append("<br/>").append(getString(
                        R.string.distance_from_current, (int) distance, mDistanceUnitShort));
            }

            markerOptions.title(user.fullname).snippet(snippet.toString());
            markerOptions.icon(mSingleUserBitmapDescriptor);
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster<ClusterUser> cluster) {
            // Render as a cluster if all the items are at the exact same location, or if there are more than
            // min_cluster_size in the cluster.
            ClusterStatus status = clusterLocationStatus(cluster);
            return status == ClusterStatus.all || status == ClusterStatus.some
                   || cluster.getSize() >= getResources().getInteger(R.integer.min_cluster_size);
        }

        /**
         * Attempt to determine the location status of items in the cluster, whether all in one location
         * or in a variety of locations.
         *
         * @param cluster
         * @return
         */
        protected ClusterStatus clusterLocationStatus(Cluster<ClusterUser> cluster) {
            HashSet<String> latLngs = new HashSet<>();
            for (ClusterUser item : cluster.getItems()) {
                latLngs.add(item.latLng.toString());
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
            StringBuilder userList = new StringBuilder();
            if (mPopup == null) {
                mPopup = mInflater.inflate(R.layout.view_user_info_multiple, null);
            }
            TextView tv = mPopup.findViewById(R.id.title);

            if (mLastClickedCluster != null) {
                double distance = calculateDistanceTo(marker.getPosition());
                TextView distance_tv = mPopup.findViewById(R.id.distance_from_current);
                distance_tv.setText(distance >= 0
                        ? Html.fromHtml(getString(
                        R.string.distance_from_current, (int) distance, mDistanceUnitShort))
                        : "");

                ArrayList<ClusterUser> users =
                        (ArrayList<ClusterUser>) mLastClickedCluster.getItems();
                Collections.sort(users, (left, right) -> {
                    int ncaLeft = left.isCurrentlyAvailable ? 0 : 1;
                    int ncaRight = right.isCurrentlyAvailable ? 0 : 1;

                    return ncaLeft != ncaRight
                            ? ncaLeft - ncaRight
                            : left.fullname.compareTo(right.fullname);

                });

                for (ClusterUser user : users) {
                    userList.append(user.fullname).append("<br/>");
                }
                userList.append(getString(R.string.click_to_view_all));

                String title = getResources().getQuantityString(R.plurals.users_at_location,
                        users.size(), users.size(), users.get(0).getLocationStr());

                tv.setText(Html.fromHtml(title));
                tv = mPopup.findViewById(R.id.snippet);
                tv.setText(Html.fromHtml(userList.toString()));
            }

            return (mPopup);
        }
    }

    /**
     * InfoWindowAdapter to present info about a single user marker.
     * Implemented here so we can have multiple lines, which the maps-provided one prevents.
     */
    class SingleUserInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        @BindView(R.id.title) TextView mLblTitle;
        @BindView(R.id.snippet) TextView mLblSnippet;

        private View mPopup = null;
        private LayoutInflater mInflater = null;

        SingleUserInfoWindowAdapter(LayoutInflater inflater) {
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
}

