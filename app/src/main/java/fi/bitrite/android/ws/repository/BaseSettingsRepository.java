package fi.bitrite.android.ws.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.ZoomedLocation;

public abstract class BaseSettingsRepository {
    public enum DistanceUnit {
        KILOMETER,
        MILES,
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public final SharedPreferences mSharedPreferences;

    private final static String KEY_MAP_LAST_LOCATION = "map_last_location";
    private final static String KEYSUFFIX_LOCATION_LATITUDE = "_latitude";
    private final static String KEYSUFFIX_LOCATION_LONGITUDE = "_longitude";
    private final static String KEYSUFFIX_LOCATION_ZOOM = "_zoom";
    private final static String HIDE_CURRENT_LOCATION_BTN = "hide_current_location_button";

    private final String mKeyDistanceUnit;
    private final String mOfflineMapEnabled;
    private final String mOfflineMapSource;
    private final String mOfflineMapStyle;
    private final String mKeyTileSource;
    private final String mKeyMessageRefreshInterval;
    private final String mKeyDataSaverMode;
    private final String mKeyDevSimulateNoNetwork;

    private final String mDefaultDistanceUnit;
    private final String mDefaultTileSource = TileSourceFactory.DEFAULT_TILE_SOURCE.name();
    private final int mDefaultMessageRefreshInterval;
    private final boolean mDefaultDataSaverMode;
    private final boolean mDefaultDevSimulateNoNetwork;

    private final float mDefaultMapLocationLatitude;
    private final float mDefaultMapLocationLongitude;
    private final int mDefaultMapLocationZoom;

    private final String mDistanceUnitKilometerRaw;
    private final String mDistanceUnitMilesRaw;
    private final String mDistanceUnitKilometerShort;
    private final String mDistanceUnitMilesShort;
    private final String mDistanceUnitKilometerLong;
    private final String mDistanceUnitMilesLong;

    BaseSettingsRepository(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        final Resources res = context.getResources();
        mKeyDistanceUnit = res.getString(R.string.prefs_distance_unit_key);

        mOfflineMapEnabled= res.getString(R.string.prefs_offline_map_enabled);
        mOfflineMapSource = res.getString(R.string.prefs_offline_map_source);
        mOfflineMapStyle = res.getString(R.string.prefs_offline_map_style);

        mKeyTileSource = res.getString(R.string.prefs_tile_source_key);
        mKeyMessageRefreshInterval = res.getString(R.string.prefs_message_refresh_interval_min_key);
        mKeyDataSaverMode = res.getString(R.string.prefs_data_saver_mode_key);
        mKeyDevSimulateNoNetwork = res.getString(R.string.prefs_dev_simulate_no_network_key);

        mDefaultDistanceUnit = res.getString(R.string.prefs_distance_unit_default);
        mDefaultMessageRefreshInterval =
                res.getInteger(R.integer.prefs_message_refresh_interval_min_default);
        mDefaultDataSaverMode = res.getBoolean(R.bool.prefs_data_saver_mode_default);
        mDefaultDevSimulateNoNetwork = res.getBoolean(R.bool.prefs_dev_simulate_no_network_default);

        mDefaultMapLocationLatitude =
                Float.parseFloat(res.getString(R.string.prefs_map_location_latitude_default));
        mDefaultMapLocationLongitude =
                Float.parseFloat(res.getString(R.string.prefs_map_location_longitude_default));
        mDefaultMapLocationZoom = res.getInteger(R.integer.prefs_map_location_zoom_default);

        mDistanceUnitKilometerRaw = res.getString(R.string.distance_unit_kilometers_short_raw);
        mDistanceUnitMilesRaw = res.getString(R.string.distance_unit_miles_short_raw);
        mDistanceUnitKilometerShort = res.getString(R.string.distance_unit_kilometers_short);
        mDistanceUnitMilesShort = res.getString(R.string.distance_unit_miles_short);
        mDistanceUnitKilometerLong = res.getString(R.string.distance_unit_kilometers_long);
        mDistanceUnitMilesLong = res.getString(R.string.distance_unit_miles_long);
    }

    public DistanceUnit getDistanceUnit() {
        String distanceUnitStr =
                mSharedPreferences.getString(mKeyDistanceUnit, mDefaultDistanceUnit);

        if (TextUtils.equals(distanceUnitStr, mDistanceUnitKilometerRaw)) {
            return DistanceUnit.KILOMETER;
        } else if (TextUtils.equals(distanceUnitStr, mDistanceUnitMilesRaw)) {
            return DistanceUnit.MILES;
        } else {
            assert false;
            return null;
        }
    }
    public String getDistanceUnitLong() {
        DistanceUnit distanceUnit = getDistanceUnit();
        switch (distanceUnit) {
            case KILOMETER: return mDistanceUnitKilometerLong;
            case MILES: return mDistanceUnitMilesLong;
            default:
                assert false;
                return null;
        }
    }
    public String getDistanceUnitShort() {
        DistanceUnit distanceUnit = getDistanceUnit();
        switch (distanceUnit) {
            case KILOMETER: return mDistanceUnitKilometerShort;
            case MILES: return mDistanceUnitMilesShort;
            default:
                assert false;
                return null;
        }
    }

    public boolean isOfflineMapEnabled() {
        return mSharedPreferences.getBoolean(mOfflineMapEnabled, false);
    }
    public File[] getOfflineMapSourceFiles() {
        Set<String> sources = mSharedPreferences.getStringSet(mOfflineMapSource, Collections.singleton(""));
        List<File> sourceFiles = new ArrayList<>();
        for (String source : sources) {
            sourceFiles.add(new File(source));
        }
        return sourceFiles.toArray(new File[0]);
    }

    public String getOfflineMapStyle() {
        return mSharedPreferences.getString(mOfflineMapStyle, "");
    }

    public String getTileSourceStr() {
        return mSharedPreferences.getString(mKeyTileSource, mDefaultTileSource);
    }

    private void setLocation(String key, ZoomedLocation position) {
        mSharedPreferences
                .edit()
                .putFloat(key + KEYSUFFIX_LOCATION_LATITUDE, (float) position.location.getLatitude())
                .putFloat(key + KEYSUFFIX_LOCATION_LONGITUDE, (float) position.location.getLongitude())
                .putFloat(key + KEYSUFFIX_LOCATION_ZOOM, (float) position.zoom)
                .apply();

    }

    public ZoomedLocation getLastMapLocation(boolean defaultIfNone) {
        if (!mSharedPreferences.contains(KEY_MAP_LAST_LOCATION + KEYSUFFIX_LOCATION_LATITUDE)
            && !defaultIfNone) {
            return null;
        }
        float latitude = mSharedPreferences.getFloat(
                KEY_MAP_LAST_LOCATION + KEYSUFFIX_LOCATION_LATITUDE, mDefaultMapLocationLatitude);
        float longitude = mSharedPreferences.getFloat(
                KEY_MAP_LAST_LOCATION + KEYSUFFIX_LOCATION_LONGITUDE, mDefaultMapLocationLongitude);
        float zoom = mSharedPreferences.getFloat(
                KEY_MAP_LAST_LOCATION + KEYSUFFIX_LOCATION_ZOOM, mDefaultMapLocationZoom);

        return new ZoomedLocation(latitude, longitude, zoom);
    }
    public void setLastMapLocation(ZoomedLocation position) {
        setLocation(KEY_MAP_LAST_LOCATION, position);
    }

    public int getMessageRefreshIntervalMin() {
        return Integer.parseInt(mSharedPreferences.getString(
                mKeyMessageRefreshInterval, Integer.toString(mDefaultMessageRefreshInterval)));
    }

    public boolean isDataSaverMode() {
        return mSharedPreferences.getBoolean(mKeyDataSaverMode, mDefaultDataSaverMode);
    }

    public boolean isDevSimulateNoNetwork() {
        return mSharedPreferences.getBoolean(
                mKeyDevSimulateNoNetwork, mDefaultDevSimulateNoNetwork);
    }

    public void setHideLocationButton(boolean hideLocationButton) {
        mSharedPreferences
                .edit()
                .putBoolean(HIDE_CURRENT_LOCATION_BTN, hideLocationButton)
                .apply();
    }

    public boolean getHideLocationButton() {
        return mSharedPreferences.getBoolean(HIDE_CURRENT_LOCATION_BTN, false);
    }

    public void registerOnChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);

        // Does an initial call with key==null.
        listener.onSharedPreferenceChanged(null, null);
    }
    public void unregisterOnChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public String getDistanceUnitKey() {
        return mKeyDistanceUnit;
    }
    public String getMessageRefreshIntervalKey() {
        return mKeyMessageRefreshInterval;
    }
    public String getDataSaverModeKey() {
        return mKeyDataSaverMode;
    }
    public String getDevSimulateNoNetworkKey() {
        return mKeyDevSimulateNoNetwork;
    }
}
