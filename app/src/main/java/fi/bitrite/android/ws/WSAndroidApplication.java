package fi.bitrite.android.ws;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import fi.bitrite.android.ws.activity.ActivityHelper;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.di.AppComponent;
import fi.bitrite.android.ws.di.AppInjector;


public class WSAndroidApplication extends Application implements HasActivityInjector {

    public static final String TAG = "WSAndroidApplication";
    private static Context mContext;
    private static AppInjector mAppInjector;

    @Inject DispatchingAndroidInjector<Activity> mDispatchingAndroidInjector;
    @Inject ActivityHelper mActivityHelper;
    @Inject AuthenticationController mAuthenticationController;

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

        mAppInjector = AppInjector.create(this);

        // Injected variables are available from this point.

        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {
                mActivityHelper.setCurrentActivity(activity);

                if (!mAuthenticationController.isInitialized()) {
                    mAuthenticationController.init();
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                mActivityHelper.setCurrentActivity(null);
            }

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
    }

    public static Context getAppContext() {
        return WSAndroidApplication.mContext;
    }
    public static AppComponent getAppComponent() {
        return mAppInjector.getAppComponent();
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return mDispatchingAndroidInjector;
    }
}
