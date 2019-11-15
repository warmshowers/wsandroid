package fi.bitrite.android.ws.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import butterknife.BindString;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.model.MapsForgeTheme;
import fi.bitrite.android.ws.repository.SettingsRepository;
import fi.bitrite.android.ws.ui.preference.RefreshIntervalPreferenceDialogFragment;
import fi.bitrite.android.ws.ui.util.ActionBarTitleHelper;
import fi.bitrite.android.ws.ui.util.OfflineMapHelper;

public class SettingsFragment extends PreferenceFragmentCompat implements Injectable,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject ActionBarTitleHelper mActionBarTitleHelper;
    @Inject SettingsRepository mSettingsRepository;

    @BindString(R.string.prefs_online_map_source) String mOnlineMapSource;

    @BindString(R.string.prefs_offline_map_enabled) String mOfflineMapSwitch;
    @BindString(R.string.prefs_offline_map_selection) String mOfflineMapSelection;
    @BindString(R.string.prefs_offline_map_source_files) String mOfflineMapSourceFiles;
    @BindString(R.string.prefs_offline_theme_selection) String mOfflineThemeSelection;
    @BindString(R.string.prefs_offline_theme_source_files) String mOfflineThemeSourceFiles;

    @BindString(R.string.prefs_offline_map_prefer_installed_locales) String mOfflineMapLanguage;

    @BindString(R.string.prefs_distance_unit_key) String mKeyDistanceUnit;
    @BindString(R.string.prefs_message_refresh_interval_min_key) String mKeyMessageRefreshInterval;

    private final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 667;

    public static Fragment create() {
        Bundle bundle = new Bundle();

        Fragment fragment = new SettingsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this, requireActivity());
    }

    @Override
    public void onResume() {
        // Injected members are only available from here.
        super.onResume();
        mActionBarTitleHelper.set(getString(R.string.title_fragment_settings));

        mSettingsRepository.registerOnChangeListener(this);


        findPreference(mOfflineMapSwitch).setSummaryProvider(
                preference -> {
                    String fileDir = OfflineMapHelper.defaultMapDataDirectory(requireContext());
                    List<String> fnames = new ArrayList<>();
                    for (File f : mSettingsRepository.getOfflineMapSourceFiles()) {
                        fnames.add(f.getAbsolutePath().replace(fileDir, ""));
                    }
                    return TextUtils.join(", ", fnames);
                }
        );

        setOfflinemapSettings();
        setSummary();
    }

    @Override
    public void onPause() {
        mSettingsRepository.unregisterOnChangeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        if (key == null) {
            return;
        }

        if (key.equals(mOfflineMapSwitch) && sharedPreferences.getBoolean(mOfflineMapSwitch, false)) {
            // ask for storage permission
            if (ContextCompat.checkSelfPermission(requireActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                searchOfflineMapData();
            }
        }
        setOfflinemapSettings();
        setSummary();
    }


    private void searchOfflineMapData() {
        OfflineMapHelper.searchOfflineMapData(mSettingsRepository,false, requireActivity());
        Set<String> availableSources = mSettingsRepository.getAvailableOfflineMapSources();
        if (availableSources.isEmpty()) {
            // no maps found. show help and uncheck button
            showMapDownloadDialog();
            ((SwitchPreference) findPreference(mOfflineMapSwitch)).setChecked(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            searchOfflineMapData();
        } else {
            ((SwitchPreference) findPreference(mOfflineMapSwitch)).setChecked(false);
        }
        setOfflinemapSettings();
    }


    private void showMapDownloadDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.no_offline_maps_found_title)
                .setMessage(Html.fromHtml(String.format(getString(R.string.no_offline_maps_found_message), OfflineMapHelper.defaultMapDataDirectory(requireContext()))))
                .setPositiveButton(R.string.alert_neutral_button, (dialog, id) -> dialog.dismiss())
                .setNeutralButton(R.string.open_map_provider_overview, ((dialog, which) -> {
                    String url = "https://github.com/mapsforge/mapsforge/blob/master/docs/Mapsforge-Maps.md";
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }))
                .setCancelable(true)
                .create()
                .show();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;
        if (TextUtils.equals(preference.getKey(), mKeyMessageRefreshInterval)) {
            dialogFragment = RefreshIntervalPreferenceDialogFragment.create(preference.getKey());
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(requireFragmentManager(), dialogFragment.getClass().getCanonicalName());
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void setOfflinemapSettings() {
        boolean offlineMapEnabled = ((SwitchPreference) findPreference(mOfflineMapSwitch)).isChecked();
        if (offlineMapEnabled) {
            listOfflineMapSources(findPreference(mOfflineMapSelection));
            listOfflineStyleSources(findPreference(mOfflineThemeSelection));
        }
        findPreference(mOfflineThemeSelection).setEnabled(offlineMapEnabled);
        findPreference(mOnlineMapSource).setEnabled(!offlineMapEnabled);

    }

    private void setSummary() {
        // online map sources
        listAvailableOnlineMapSources(findPreference(mOnlineMapSource));

        // message refresh interval
        Resources res = getResources();
        int intervalMin = mSettingsRepository.getMessageRefreshIntervalMin();
        findPreference(mKeyMessageRefreshInterval).setSummary(intervalMin > 0
                ? res.getQuantityString(R.plurals.prefs_message_refresh_interval_min_summary,
                intervalMin, intervalMin)
                : getString(R.string.prefs_message_refresh_interval_min_summary_disabled));

        // distance units
        findPreference(mKeyDistanceUnit).setSummary(getString(
                R.string.prefs_distance_unit_summary,
                mSettingsRepository.getDistanceUnitLong()));
    }

    private void listOfflineMapSources(final MultiSelectListPreference pref) {
        Set<String> maps = mSettingsRepository.getAvailableOfflineMapSources();

        final List<String> sourceNames = new LinkedList<>();
        final List<String> sourceValues = new LinkedList<>();
        for (String map: maps) {
            File f = new File(map);
            if (f.exists()) {
                sourceNames.add(f.getName());
                sourceValues.add(f.getAbsolutePath());
            }
        }

        pref.setEntries(sourceNames.toArray(new CharSequence[0]));
        pref.setEntryValues(sourceValues.toArray(new CharSequence[0]));
        if (pref.getValues().isEmpty() && pref.getEntryValues().length > 0) {
            // use all maps as default
            Set<String> values = new HashSet<>();
            for (CharSequence cs : pref.getEntryValues()) {
                values.add(cs.toString());
            }
            pref.setValues(values);
        }
    }

    private void listOfflineStyleSources(final ListPreference pref) {
        List<MapsForgeTheme> themes = mSettingsRepository.getAvailableOfflineThemeSources();

        final List<String> sourceNames = new LinkedList<>();
        final List<String> sourceValues = new LinkedList<>();
        Gson gson = new Gson();
        for (MapsForgeTheme theme : themes) {
            if (new File(theme.getFilePath()).exists() || theme.getId().equals("default_theme")) {
                sourceNames.add(theme.getLocalizedDisplayName(Locale.getDefault().getLanguage()));
                sourceValues.add(gson.toJson(theme));
            }
        }

        pref.setEntries(sourceNames.toArray(new CharSequence[0]));
        pref.setEntryValues(sourceValues.toArray(new CharSequence[0]));
        if (pref.getValue() == null) {
            // if user has selected nothing, use default theme
            pref.setSummary(themes.get(themes.size() - 1).getLocalizedDisplayName(Locale.getDefault().getLanguage()));
            pref.setValue(pref.getEntryValues()[0].toString());
        }
    }

    private void listAvailableOnlineMapSources(final ListPreference tileSourcePreference) {
        tileSourcePreference.setSummary(getString(
                R.string.prefs_tile_source_summary,
                mSettingsRepository.getOnlineMapSourceStr()));
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

        // Some maps only cover the US
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
                tileSourceNames.toArray(new CharSequence[0]);
        CharSequence[] tileSourceValuesCS =
                tileSourceValues.toArray(new CharSequence[0]);

        tileSourcePreference.setEntries(tileSourceNamesCS);
        tileSourcePreference.setEntryValues(tileSourceValuesCS);
        tileSourcePreference.setDefaultValue(TileSourceFactory.DEFAULT_TILE_SOURCE.name());
    }
}
