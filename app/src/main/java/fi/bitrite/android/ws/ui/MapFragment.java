package fi.bitrite.android.ws.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.StaticCluster;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.api.response.UserSearchByLocationResponse;
import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.model.ZoomedLocation;
import fi.bitrite.android.ws.repository.BaseSettingsRepository;
import fi.bitrite.android.ws.repository.FavoriteRepository;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.repository.SettingsRepository;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.listadapter.UserListAdapter;
import fi.bitrite.android.ws.ui.util.NavigationController;
import fi.bitrite.android.ws.ui.util.UserMarker;
import fi.bitrite.android.ws.ui.util.UserMarkerClusterer;
import fi.bitrite.android.ws.util.LocationManager;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import fi.bitrite.android.ws.util.Tools;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class MapFragment extends BaseFragment {
    private static final String KEY_MAP_TARGET_LAT_LNG = "map_target_lat_lng";
    private static final String TAG = "MapFragment";

    @Inject LoggedInUserHelper mLoggedInUserHelper;
    @Inject UserRepository mUserRepository;
    @Inject FavoriteRepository mFavoriteRepository;
    @Inject SettingsRepository mSettingsRepository;

    @BindColor(R.color.primaryColor) int mColorPrimary;
    @BindColor(R.color.primaryWhite) int mColorPrimaryWhite;
    @BindColor(R.color.primaryButtonDisable) int mColorPrimaryButtonDisable;
    @BindDrawable(R.drawable.ic_my_location_white_24dp) Drawable mIcMyLocationWhite;
    @BindDrawable(R.drawable.ic_my_location_grey600_24dp) Drawable mIcMyLocationGrey;
    @BindView(R.id.map) MapView mMap;
    @BindView(R.id.map_btn_goto_current_location) FloatingActionButton mBtnGotoCurrentLocation;
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
    private boolean mOsmdroidBug_suppressCallbacks;
    private final LocationManager mLocationManager = new LocationManager();

    private boolean mHasEnabledLocationProviders;
    private final BehaviorSubject<Location> mLastDeviceLocation = BehaviorSubject.create();

    private MyLocationNewOverlay mDeviceLocationOverlay;
    private int mapZoomMinLoad;

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

        UserMarkerClusterer.MarkerFactory singleLocationMarkerFactory =
                new UserMarkerClusterer.MarkerFactory(
                        getResources().getDrawable(R.drawable.map_markers_multiple));
        singleLocationMarkerFactory.setTextAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
        singleLocationMarkerFactory.setTextPadding(0, 15);

        UserMarkerClusterer.MarkerFactory multiLocationMarkerFactory =
                new UserMarkerClusterer.MarkerFactory(
                        getResources().getDrawable(R.drawable.ic_cluster_multi_location_38dp));

        mMarkerClusterer = new UserMarkerClusterer(getContext());
        mMarkerClusterer.setSingleLocationMarkerFactory(singleLocationMarkerFactory);
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

        String tileSourceStr = mSettingsRepository.getTileSourceStr();
        if (!TileSourceFactory.containsTileSource(tileSourceStr)) {
            tileSourceStr = TileSourceFactory.DEFAULT_TILE_SOURCE.name();
        }
        mMap.setTileSource(TileSourceFactory.getTileSource(tileSourceStr));

        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            handleAccessFineLocationGranted();
            // The else case is handled in onResume.
        }

        mapZoomMinLoad = getResources().getInteger(R.integer.map_zoom_min_load);
        mMap.getOverlays().add(mMarkerClusterer);
        addScaleBarOverlay();

        mMap.addOnFirstLayoutListener((v, left, top, right, bottom) -> {
            // We add this only here due to issues in osmdroid's setCenter/setZoom when the first
            // layout of the map is not yet done.
            mMap.addMapListener(new MapListener() {
                @Override
                public boolean onScroll(ScrollEvent event) {
                    if (mOsmdroidBug_suppressCallbacks) {
                        return false;
                    }
                    onPositionChange(mMap.getZoomLevelDouble());
                    return true;
                }
                @Override
                public boolean onZoom(ZoomEvent event) {
                    if (mOsmdroidBug_suppressCallbacks) {
                        return false;
                    }
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

        // Adds a button to navigate to the current GPS position.
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, 0);
        } else {
            startLocationManager();
        }

        getResumePauseDisposable().add(mLocationManager.getHasEnabledProviders()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(hasEnabledProviders -> {
                    mHasEnabledLocationProviders = hasEnabledProviders;
                    setGotoCurrentLocationStatus();
                }));
        getResumePauseDisposable().add(mLocationManager.getBestLocation()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(location -> {
                    mLastDeviceLocation.onNext(location);
                    // FIXME(saemy): Clear cluster info window cache as the distance to this distance is not
                    //               re-calculated.

                    setGotoCurrentLocationStatus();

                    // As we know more location details, we do (another) initial map move. This does not
                    // affect the current location, if we already moved to a more detailed location.
                    doInitialMapMove();
                }));
    }

    @Override
    public void onPause() {
        mLocationManager.stop();
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
        mDeviceLocationOverlay = new MyLocationNewOverlay(mLocationProvider, mMap);
        mDeviceLocationOverlay.enableMyLocation();
        mDeviceLocationOverlay.setDrawAccuracyEnabled(true);
        mDeviceLocationOverlay.getEnableAutoStop(); // Stop following location on map move by user.
        mDeviceLocationOverlay.disableFollowLocation(); // Initially do not follow the current location.
        mDeviceLocationOverlay.setOptionsMenuEnabled(true);
        mMap.getOverlays().add(mDeviceLocationOverlay);

        startLocationManager();
    }

    private void startLocationManager() {
        mLocationManager.start((android.location.LocationManager) getActivity().getSystemService(
                Context.LOCATION_SERVICE));
    }

    @OnClick(R.id.map_btn_goto_current_location)
    void onGotoCurrentLocationClicked() {
        mDeviceLocationOverlay.enableFollowLocation(); // Follow the current location.
        if (mLastDeviceLocation.getValue() == null) {
            Toast.makeText(getContext(), R.string.unknown_location, Toast.LENGTH_SHORT)
                    .show();
        } else {
            double zoom = Math.max(13, Math.min(17, mMap.getZoomLevelDouble())); // zoom \in [13,17]
            moveMapToLocation(Tools.locationToLatLng(mLastDeviceLocation.getValue()), zoom,
                    POSITION_PRIORITY_FORCED);
        }
    }

    private void setGotoCurrentLocationStatus() {
        mBtnGotoCurrentLocation.setEnabled(mLastDeviceLocation.getValue() != null
                                           || mHasEnabledLocationProviders);

        int fillColor;
        Drawable icon;
        if (mLastDeviceLocation.getValue() != null) {
            icon = mIcMyLocationWhite;
            fillColor = mColorPrimary;
        } else if (mHasEnabledLocationProviders) {
            icon = mIcMyLocationGrey;
            fillColor = mColorPrimaryWhite;
        } else {
            icon = mIcMyLocationWhite;
            fillColor = mColorPrimaryButtonDisable;
        }
        mBtnGotoCurrentLocation.setImageDrawable(icon);
        mBtnGotoCurrentLocation.setBackgroundTintList(ColorStateList.valueOf(fillColor));
    }

    private void addScaleBarOverlay() {
        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMap);

        // Place at bottom left with same margins as FAB
        int offset = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                getResources().getDisplayMetrics()));
        mScaleBarOverlay.setAlignBottom(true);
        mScaleBarOverlay.setScaleBarOffset(offset, offset);
        mScaleBarOverlay.setMinZoom(mapZoomMinLoad);

        // Use distance unit set in prefs
        mDistanceUnit = mSettingsRepository.getDistanceUnit();
        ScaleBarOverlay.UnitsOfMeasure unit = (
                mDistanceUnit == BaseSettingsRepository.DistanceUnit.MILES)
                ? ScaleBarOverlay.UnitsOfMeasure.imperial
                : ScaleBarOverlay.UnitsOfMeasure.metric;
        mScaleBarOverlay.setUnitsOfMeasure(unit);

        mMap.getOverlays().add(mScaleBarOverlay);
    }

    /**
     * Moves the map to the given location if the given priority is newer than the current one.
     * A default value for zoom is used if the given value is less than zero.
     */
    private void moveMapToLocation(IGeoPoint center, double zoom, int positionPriority) {
        if (center == null) {
            return;
        }

        if (mLastPositionType < positionPriority || positionPriority == POSITION_PRIORITY_FORCED) {
            mLastPositionType = positionPriority;

            if (zoom < 0) {
                zoom = getResources().getInteger(R.integer.prefs_map_location_zoom_default);
            }
            // FIXME(saemy): osmdroid does not provide a possibility to just move to a center+zoom
            // which issues the callbacks only once. So we suppress any spurious callback calls.
            mOsmdroidBug_suppressCallbacks = true;
            mMapController.setZoom(zoom);
            mOsmdroidBug_suppressCallbacks = false;
            mMapController.setCenter(center);
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

        if (mLastDeviceLocation.getValue() != null) {
            moveMapToLocation(Tools.locationToLatLng(mLastDeviceLocation.getValue()), showUserZoom,
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
        mLastPosition = new ZoomedLocation(mapCenter.getLatitude(), mapCenter.getLongitude(), zoom);

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
        if (mLastPosition.zoom < mapZoomMinLoad) {
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
                mMarkerClusterer.remove(existingMarker);
            }

            UserMarker marker = new UserMarker(getContext(), mMap, user);
            marker.setAnchor(UserMarker.ANCHOR_CENTER, UserMarker.ANCHOR_BOTTOM);
            marker.setIcon(getResources().getDrawable(R.drawable.map_markers_single));
            marker.setOnMarkerClickListener((m, mapView) -> {
                // We need a new ArrayList here, as it gets sorted in {@link UserListAdapter} and
                // Collections.singletonList() provides a non-mutable list.
                new MultiUserSelectDialog().show(new ArrayList<>(Collections.singletonList(user)));
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
        return mLastDeviceLocation.getValue() != null
                ? Tools.calculateDistanceBetween(
                Tools.latLngToLocation(latLng), mLastDeviceLocation.getValue(), mDistanceUnit)
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

    private IMyLocationProvider mLocationProvider = new IMyLocationProvider() {
        private Disposable mDisposable;

        @Override
        public boolean startLocationProvider(IMyLocationConsumer locationConsumer) {
            if (locationConsumer == null) {
                return false;
            }
            mDisposable = mLastDeviceLocation
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(location -> locationConsumer.onLocationChanged(location, this));
            return true;
        }
        @Override
        public void stopLocationProvider() {
            mDisposable.dispose();
        }
        @Override
        public Location getLastKnownLocation() {
            return mLastDeviceLocation.getValue();
        }
        @Override
        public void destroy() {
        }
    };
}

