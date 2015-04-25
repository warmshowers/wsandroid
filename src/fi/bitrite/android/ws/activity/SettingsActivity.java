package fi.bitrite.android.ws.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;

import fi.bitrite.android.ws.R;

public class SettingsActivity
        extends WSBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initView();

        // Fragment approach thanks to http://stackoverflow.com/a/26564401/215713
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new WsPreferenceFragment()).commit();
    }


    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

}
