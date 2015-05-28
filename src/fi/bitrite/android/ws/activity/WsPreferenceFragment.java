package fi.bitrite.android.ws.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
        //ListPreference pref = (ListPreference) findPreference("distance_unit");
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
