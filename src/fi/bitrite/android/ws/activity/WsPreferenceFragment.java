package fi.bitrite.android.ws.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;

import fi.bitrite.android.ws.R;

/**
 * This fragment approach is thanks to http://stackoverflow.com/a/26564401/215713
 */
public class WsPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {


    @Override
    public void onCreate(final Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        setSummary();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary();
    }
    void setSummary() {
        MaterialListPreference pref = (MaterialListPreference) findPreference("distance_unit");
        CharSequence title = pref.getEntry();
        pref.setSummary(title);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }
}
