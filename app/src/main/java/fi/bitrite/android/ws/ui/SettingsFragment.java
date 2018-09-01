package fi.bitrite.android.ws.ui;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.repository.SettingsRepository;
import fi.bitrite.android.ws.ui.preference.RefreshIntervalPreferenceDialogFragment;
import fi.bitrite.android.ws.ui.util.ActionBarTitleHelper;

public class SettingsFragment extends PreferenceFragmentCompat implements Injectable,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject ActionBarTitleHelper mActionBarTitleHelper;
    @Inject SettingsRepository mSettingsRepository;

    @BindString(R.string.prefs_distance_unit_key) String mKeyDistanceUnit;
    @BindString(R.string.prefs_tile_source_key) String mTileMapSource;
    @BindString(R.string.prefs_message_refresh_interval_min_key) String mKeyMessageRefreshInterval;

    public static Fragment create() {
        Bundle bundle = new Bundle();

        Fragment fragment = new SettingsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this, getActivity());
    }

    @Override
    public void onResume() {
        // Injected members are only available from here.
        super.onResume();
        mActionBarTitleHelper.set(getString(R.string.title_fragment_settings));

        mSettingsRepository.registerOnChangeListener(this);
        setSummary();
    }

    @Override
    public void onPause() {
        mSettingsRepository.unregisterOnChangeListener(this);
        super.onPause();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        if (BuildConfig.DEBUG) {
            addPreferencesFromResource(R.xml.developer_preferences);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary();
    }

    private void setSummary() {
        // distance units
        findPreference(mKeyDistanceUnit).setSummary(getString(
                R.string.prefs_distance_unit_summary,
                mSettingsRepository.getDistanceUnitLong()));

        // map sources
        setAvailableMapSources((ListPreference) findPreference(mTileMapSource));

        // message refresh interval
        Resources res = getResources();
        int intervalMin = mSettingsRepository.getMessageRefreshIntervalMin();
        findPreference(mKeyMessageRefreshInterval).setSummary(intervalMin > 0
                ? res.getQuantityString(R.plurals.prefs_message_refresh_interval_min_summary,
                intervalMin, intervalMin)
                : getString(R.string.prefs_message_refresh_interval_min_summary_disabled));
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;
        if (TextUtils.equals(preference.getKey(), mKeyMessageRefreshInterval)) {
            dialogFragment = RefreshIntervalPreferenceDialogFragment.create(preference.getKey());
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), dialogFragment.getClass().getCanonicalName());
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void setAvailableMapSources(final ListPreference tileSourcePreference) {
        tileSourcePreference.setSummary(getString(
                R.string.prefs_tile_source_summary,
                mSettingsRepository.getTileSourceStr()));
        final List<String> tileSourceNames = new LinkedList<>();
        final List<String> tileSourceValues = new LinkedList<>();
        final List<ITileSource> tileSources = TileSourceFactory.getTileSources();

        // Blacklist unused sources
        List<ITileSource> blacklisted_sources = Arrays.asList(
                TileSourceFactory.PUBLIC_TRANSPORT,  // overlay only
                TileSourceFactory.ChartbundleENRL,   // Chartbundle US Aviation Charts
                TileSourceFactory.ChartbundleENRH,
                TileSourceFactory.ChartbundleWAC);
        tileSources.removeAll(blacklisted_sources);

        // Some maps are only available in the US
        List<ITileSource> usOnlySources = Arrays.asList(
                TileSourceFactory.USGS_SAT,
                TileSourceFactory.USGS_TOPO);
        tileSources.removeAll(usOnlySources);

        Collections.sort(tileSources, (o1, o2) -> o1.name().compareTo(o2.name()));

        // Add regular map sources first
        for (ITileSource tileSource : tileSources) {
            tileSourceNames.add(tileSource.name());
            tileSourceValues.add(tileSource.name());
        }

        // Add us-only sources
        for (ITileSource tileSource : usOnlySources) {
            // Shorten name and add a notice: "USGS National Map Topo" -> "USGS Topo (US only)"
            String name = tileSource.name().replace("National Map ", "");
            tileSourceNames.add(getString(R.string.prefs_tile_source_us_only, name));
            tileSourceValues.add(tileSource.name());
        }

        CharSequence[] tileSourceNamesCS =
                tileSourceNames.toArray(new CharSequence[tileSourceNames.size()]);
        CharSequence[] tileSourceValuesCS =
                tileSourceValues.toArray(new CharSequence[tileSourceValues.size()]);

        tileSourcePreference.setEntries(tileSourceNamesCS);
        tileSourcePreference.setEntryValues(tileSourceValuesCS);
        tileSourcePreference.setDefaultValue(TileSourceFactory.DEFAULT_TILE_SOURCE.name());
    }
}
