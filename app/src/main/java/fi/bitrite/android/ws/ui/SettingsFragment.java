package fi.bitrite.android.ws.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceFragmentCompat;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.repository.SettingsRepository;
import fi.bitrite.android.ws.ui.util.ActionBarTitleHelper;

public class SettingsFragment extends PreferenceFragmentCompat implements Injectable,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject ActionBarTitleHelper mActionBarTitleHelper;
    @Inject SettingsRepository mSettingsRepository;

    @BindString(R.string.prefs_distance_unit_key) String mKeyDistanceUnit;

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
        findPreference(mKeyDistanceUnit).setSummary(getString(
                R.string.prefs_distance_unit_summary,
                mSettingsRepository.getDistanceUnitLong()));
    }
}
