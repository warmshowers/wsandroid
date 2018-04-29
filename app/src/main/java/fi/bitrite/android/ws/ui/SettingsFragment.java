package fi.bitrite.android.ws.ui;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;

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
        findPreference(mKeyDistanceUnit).setSummary(getString(
                R.string.prefs_distance_unit_summary,
                mSettingsRepository.getDistanceUnitLong()));

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
            dialogFragment = RefreshIntervalPreferenceDialogFragment
                    .create(preference.getKey());

        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), dialogFragment.getClass().getCanonicalName());
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
