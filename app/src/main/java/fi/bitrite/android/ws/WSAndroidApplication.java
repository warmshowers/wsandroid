package fi.bitrite.android.ws;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;


public class WSAndroidApplication extends Application {

    public static final String TAG = "WSAndroidApplication";
    private static Context mContext;

    // Google Analytics Support
    public enum TrackerName {
        APP_TRACKER,
        GLOBAL_TRACKER
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public synchronized Tracker getTracker(TrackerName trackerId) {
        String PROPERTY_ID = getString(R.string.ga_property_id);

        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(R.xml.app_tracker)
                    : (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(R.xml.global_tracker) :
                    analytics.newTracker(PROPERTY_ID);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }


    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();

        // Set automatic activity reports, per http://stackoverflow.com/a/24983778/215713
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        analytics.enableAutoActivityReports(this);

        boolean gaOptOut = !PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("ga_collect_stats", true);
        analytics.setAppOptOut(gaOptOut);

    }

    public static Context getAppContext() {
        return WSAndroidApplication.mContext;
    }


}
