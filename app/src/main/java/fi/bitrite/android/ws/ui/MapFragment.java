package fi.bitrite.android.ws.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.common.collect.Lists;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.StaticCluster;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.api.response.UserSearchByLocationResponse;
import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.model.ZoomedLocation;
import fi.bitrite.android.ws.repository.FavoriteRepository;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.repository.SettingsRepository;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.listadapter.UserListAdapter;
import fi.bitrite.android.ws.ui.util.NavigationController;
import fi.bitrite.android.ws.ui.util.UserMarker;
import fi.bitrite.android.ws.ui.util.UserMarkerClusterer;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import fi.bitrite.android.ws.util.Tools;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class MapFragment extends BaseFragment {
    private static final String KEY_MAP_TARGET_LAT_LNG = "map_target_lat_lng";
    private static final String TAG = "MapFragment";

    @Inject LoggedInUserHelper mLoggedInUserHelper;
    @Inject UserRepository mUserRepository;
    @Inject FavoriteRepository mFavoriteRepository;
    @Inject SettingsRepository mSettingsRepository;

    @BindView(R.id.map) MapView mMap;
    private IMapController mMapController;

    private Unbinder mUnbinder;

    private SparseArray<Marker> mClusteredUsers = new SparseArray<>();
    private UserMarkerClusterer mMarkerClusterer;

    private Disposable mLoadOfflineUserDisposable;
    private final List<Integer> mOfflineUserIds = new ArrayList<>();

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

    private int mLastPositionType;
    private ZoomedLocation mLastPosition;
    private ZoomedLocation mOsmdroidBug1055_positionBeingSet;
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
    public static MapFragment create(IGeoPoint latLng) {
        MapFragment mapFragment = create();
        mapFragment.getArguments().putParcelable(KEY_MAP_TARGET_LAT_LNG,
                new GeoPoint(latLng.getLatitude(), latLng.getLongitude()));
        return mapFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());


        UserMarkerClusterer.MarkerFactory singleLocationMarkerFactory =
                new UserMarkerClusterer.MarkerFactory(
                        getResources().getDrawable(R.drawable.map_markers_multiple));
        singleLocationMarkerFactory.setTextAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
        singleLocationMarkerFactory.setTextPadding(0, 15);

        UserMarkerClusterer.MarkerFactory multiLocationMarkerFactory =
                new UserMarkerClusterer.MarkerFactory(
                        getResources().getDrawable(R.drawable.ic_cluster_multi_location_38dp));

        mMarkerClusterer = new UserMarkerClusterer(getContext());
        mMarkerClusterer.setmSingleLocationMarkerFactory(singleLocationMarkerFactory);
        mMarkerClusterer.setMultiLocationMarkerFactory(multiLocationMarkerFactory);
        mMarkerClusterer.setOnClusterClickListener(this::onClusterClick);

        loadOfflineUsers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        Configuration.getInstance().load(
                context, PreferenceManager.getDefaultSharedPreferences(context));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        mMapController = mMap.getController();

        mLastPositionType = -1;
        mLastPosition = null;

        mMap.setVerticalMapRepetitionEnabled(false);
        mMap.setBuiltInZoomControls(false);
        mMap.setMultiTouchControls(true);

        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            handleAccessFineLocationGranted();
            // The else case is handled in onResume.
        }

        mMap.getOverlays().add(mMarkerClusterer);

        mMap.addOnFirstLayoutListener((v, left, top, right, bottom) -> {
            // We add this only here due to issues in osmdroid's setCenter/setZoom when the first
            // layout of the map is not yet done.
            mMap.addMapListener(new MapListener() {
                @Override
                public boolean onScroll(ScrollEvent event) {
                    onPositionChange(mMap.getZoomLevelDouble());
                    return true;
                }
                @Override
                public boolean onZoom(ZoomEvent event) {
                    onPositionChange(event.getZoomLevel());
                    return true;
                }
            });

            doInitialMapMove();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mMap.onResume();

        // Register the settings change listener. That does an initial call to the handler.
        mSettingsRepository.registerOnChangeListener(mOnSettingsChangeListener);

        LocationRequest locationRequest = new LocationRequest()
                .setInterval(60000)
                .setFastestInterval(60000)
                .setPriority(LocationRequest.PRIORITY_LOW_POWER);
        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);

        // Adds a button to navigate to the current GPS position.
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, 0);
        }
    }

    @Override
    public void onPause() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        mSettingsRepository.unregisterOnChangeListener(mOnSettingsChangeListener);
        mMap.onPause();

        super.onPause();
    }

    @Override
    public void onStop() {
        if (mLastPosition != null) {
            mSettingsRepository.setLastMapLocation(mLastPosition);
        }

        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
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
            handleAccessFineLocationGranted();
        }
    }
    private void handleAccessFineLocationGranted() {
        MyLocationNewOverlay overlay =
                new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), mMap);
        overlay.enableMyLocation();
        overlay.setDrawAccuracyEnabled(true);
        overlay.enableFollowLocation();
        overlay.setOptionsMenuEnabled(true);
        mMap.getOverlays().add(overlay);
    }

    /**
     * Moves the map to the given location if the given priority is newer than the current one.
     * A default value for zoom is used if the given value is less than zero.
     */
    private void moveMapToLocation(IGeoPoint center, double zoom, int positionPriority) {
        if (center == null) {
            return;
        }

        if (mLastPositionType < positionPriority) {
            mLastPositionType = positionPriority;

            if (zoom < 0) {
                zoom = getResources().getInteger(R.integer.prefs_map_location_zoom_default);
            }
            // FIXME(saemy): This osmdroid code is buggy as mMap.isLayoutOccured() is false and
            // therefore the center is not correctly returned in the listener to these changes...
            mOsmdroidBug1055_positionBeingSet = new ZoomedLocation(center, zoom);
            mMapController.setZoom(zoom);
            mMapController.setCenter(center);
            mOsmdroidBug1055_positionBeingSet = null;
        }
    }
    private void doInitialMapMove() {
        float showUserZoom = getResources().getInteger(R.integer.map_showuser_zoom);

        // If we were launched with an intent asking us to zoom to a member
        IGeoPoint targetLatLng = getArguments().getParcelable(KEY_MAP_TARGET_LAT_LNG);
        if (targetLatLng != null) {
            moveMapToLocation(targetLatLng, showUserZoom, POSITION_PRIORITY_FORCED);
            return;
        }

        // Fetches the last location from the settings (but no default yet).
        ZoomedLocation savedLocation = mSettingsRepository.getLastMapLocation(false);
        if (savedLocation != null) {
            moveMapToLocation(
                    savedLocation.location, savedLocation.zoom, POSITION_PRIORITY_LAST_STORED);
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
        moveMapToLocation(savedLocation.location, savedLocation.zoom, POSITION_PRIORITY_ESTIMATE);
    }

    private final static long FETCH_USERS_DELAY_MS = 700;
    private Disposable mDelayedUserFetchDisposable;
    private void onPositionChange(double zoom) {
        IGeoPoint mapCenter = mMap.getMapCenter();
        mLastPosition = mOsmdroidBug1055_positionBeingSet != null
                ? mOsmdroidBug1055_positionBeingSet
                : new ZoomedLocation(mapCenter.getLatitude(), mapCenter.getLongitude(), zoom);

        // If not connected, we'll switch to offline/starred users mode
        if (!Tools.isNetworkConnected(getContext())) {
            sendMessage(R.string.map_network_not_connected);
            return;
        }

        // We delay the execution for some time as we get a burst of updates once the user moves the
        // map.
        if (mDelayedUserFetchDisposable != null) {
            mDelayedUserFetchDisposable.dispose();
        }
        mDelayedUserFetchDisposable = Completable.complete()
                .delay(FETCH_USERS_DELAY_MS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::fetchUsersForCurrentMapPosition);
        getResumePauseDisposable().add(mDelayedUserFetchDisposable);
    }
    private void fetchUsersForCurrentMapPosition() {
        if (mLastPosition.zoom < getResources().getInteger(R.integer.map_zoom_min_load)) {
            sendMessage(R.string.users_dont_load);
        } else {
            sendMessage(R.string.loading_users);

            getResumePauseDisposable().add(
                    mUserRepository.searchByLocation(mMap.getBoundingBox())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(searchResult -> {
                                if (searchResult.isEmpty()) {
                                    return;
                                }

                                for (UserSearchByLocationResponse.User user : searchResult) {
                                    addUserToCluster(user.toSimpleUser());
                                }
                                mMarkerClusterer.invalidate();
                                mMap.invalidate();
                            }, throwable -> {
                                // TODO(saemy): Error handling.
                                Log.e(TAG, throwable.getMessage());
                            }));
        }

    }

    private void loadOfflineUsers() {
        // We'll show the starred users until we load a fresh version of them.
        mLoadOfflineUserDisposable = Observable.merge(mFavoriteRepository.getFavorites())
                .filter(Resource::hasData)
                .map(userResource -> userResource.data)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    // Users pop up twice as one is the error since we might not be able to load it
                    // from the network.
                    boolean added = mOfflineUserIds.add(user.id);
                    if (!added) {
                        return;
                    }

                    addUserToCluster(user);
                    mMarkerClusterer.invalidate();
                });
        getCreateDestroyDisposable().add(mLoadOfflineUserDisposable);
    }

    private void addUserToCluster(SimpleUser user) {
        // Only add to the cluster if it wasn't before or when its location changed.
        final Marker existingMarker = mClusteredUsers.get(user.id);
        boolean isNew = existingMarker == null
                        || !existingMarker.getPosition().equals(user.location);
        if (isNew) {
            if (existingMarker != null) {
                // FIXME(saemy): Remove the old marker.
            }

            UserMarker marker = new UserMarker(getContext(), mMap, user);
            marker.setAnchor(UserMarker.ANCHOR_CENTER, UserMarker.ANCHOR_BOTTOM);
            marker.setIcon(getResources().getDrawable(R.drawable.map_markers_single));
            marker.setOnMarkerClickListener((m, mapView) -> {
                new MultiUserSelectDialog().show(Lists.newArrayList(user));
                return true;
            });
            mMarkerClusterer.add(marker);
            mClusteredUsers.put(user.id, marker);
        }
    }

    /**
     * - Capture the clicked cluster so we can use it in custom infoWindow
     * - Check overall bounds of items in cluster
     * - If the bounds are empty (all users at same place) then let it pop the info window
     * - Otherwise, move the camera to show the bounds of the map
     */
    public boolean onClusterClick(MapView mapView, StaticCluster cluster) {
        // Find out the bounds of the users currently in cluster
        List<SimpleUser> users = new ArrayList<>(cluster.getSize());
        List<IGeoPoint> locations = new ArrayList<>(cluster.getSize());
        for (int i = 0; i < cluster.getSize(); ++i) {
            UserMarker userMarker = (UserMarker) cluster.getItem(i);
            users.add(userMarker.getUser());
            locations.add(userMarker.getPosition());
        }
        BoundingBox bounds = BoundingBox.fromGeoPoints(locations);

        // If the users are not all at the same location, then change bounds of map.
        if (bounds.getDiagonalLengthInMeters() > 0) { // TODO(saemy): something bigger than 0?
            // Offset from edge of map in pixels when exploding cluster
            int padding_percent =
                    getResources().getInteger(R.integer.cluster_explode_padding_percent);
            int padding = Math.min(mapView.getHeight(), mapView.getWidth()) * padding_percent / 100;
            mapView.zoomToBoundingBox(bounds, true, padding);
        } else {
            new MultiUserSelectDialog().show(users);
        }

        return true;
    }

    /**
     * Returns the distance to given point or -1 if the current position is unknown.
     */
    private double calculateDistanceTo(IGeoPoint latLng) {
        return mLastDeviceLocation != null
                ? Tools.calculateDistanceBetween(
                Tools.latLngToLocation(latLng), mLastDeviceLocation, mDistanceUnit)
                : -1;
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
        return getString(R.string.app_name);
    }

    class MultiUserSelectDialog {
        @BindView(R.id.title) TextView mTxtTitle;
        @BindView(R.id.distance_from_current) TextView mTxtDistanceFromCurrent;

        void show(final List<? extends SimpleUser> users) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            Context dialogContext = dialogBuilder.getContext();

            View titleView = null;
            if (users.size() > 1) {
                titleView = LayoutInflater.from(dialogContext).inflate(
                        R.layout.view_multiuser_dialog_header, null, false);
                ButterKnife.bind(this, titleView);

                final SimpleUser representative = users.get(0);
                mTxtTitle.setText(getResources().getQuantityString(R.plurals.users_at_location,
                        users.size(), users.size(), representative.getStreetCityAddress()));

                double distance = calculateDistanceTo(representative.location);
                String distanceSummary = getString(
                        R.string.distance_from_current, (int) distance, mDistanceUnitShort);
                mTxtDistanceFromCurrent.setText(distanceSummary);
                mTxtDistanceFromCurrent.setVisibility(distance >= 0 ? View.VISIBLE : View.GONE);
            }

            UserListAdapter userListAdapter = new UserListAdapter(
                    dialogContext, UserListAdapter.COMPERATOR_FULLNAME_ASC, null);
            userListAdapter.resetDataset(users);
            // Remember the navigationController here as sometimes the activity is no longer set
            // in the listener.
            final NavigationController navigationController = getNavigationController();
            dialogBuilder
                    .setCustomTitle(titleView)
                    .setNegativeButton(R.string.ok, (dialog, which) -> {})
                    .setAdapter(userListAdapter, (dialog, index) -> {
                        SimpleUser user = users.get(index);
                        navigationController.navigateToUser(user.id);
                    })
                    .create()
                    .show();
        }
    }
}

