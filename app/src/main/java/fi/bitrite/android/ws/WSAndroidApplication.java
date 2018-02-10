package fi.bitrite.android.ws;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import fi.bitrite.android.ws.api.AuthenticationController;
import fi.bitrite.android.ws.di.AppComponent;
import fi.bitrite.android.ws.di.AppInjector;

public class WSAndroidApplication extends Application implements HasActivityInjector {

    public static final String TAG = "WSAndroidApplication";
    private static Context mContext;
    private static AppInjector mAppInjector;

    @Inject DispatchingAndroidInjector<Activity> mDispatchingAndroidInjector;
    @Inject AuthenticationController mAuthenticationController;
    HashMap<TrackerName, Tracker> mTrackers = new HashMap<>();

    public static Context getAppContext() {
        return WSAndroidApplication.mContext;
    }

    public static AppComponent getAppComponent() {
        return mAppInjector.getAppComponent();
    }

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

        mAppInjector = AppInjector.create(this);

        // Injected variables are available from this point.
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return mDispatchingAndroidInjector;
    }

    // Google Analytics Support
    public enum TrackerName {
        APP_TRACKER,
        GLOBAL_TRACKER
    }
}
