package fi.bitrite.android.ws.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;

import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.Tools;

/**
 * This fragment approach is thanks to http://stackoverflow.com/a/26564401/215713
 */
public class WsPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {


    @Override
    public void onCreate(final Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        if (BuildConfig.DEBUG) {
            addPreferencesFromResource(R.xml.developer_options);
        }

        setSummary();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary();
        switch (key) {
            case "developer_server_url":
                GlobalInfo.setWarmshowersBaseUrl(sharedPreferences.getString(key, "https://www.warmshowers.org"));
                break;
            case "developer_server_cookie":
                GlobalInfo.setWarmshowersCookieDomain(sharedPreferences.getString(key, ".warmshowers.org"));
                break;
        }
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
