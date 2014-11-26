package fi.bitrite.android.ws.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import fi.bitrite.android.ws.R;

public class SettingsActivity
        extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        setSummary();
    }

    void setSummary() {
        ListPreference pref = (ListPreference)findPreference("distance_unit");
        CharSequence title = pref.getEntry();
        pref.setSummary(title);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary();
    }

}
