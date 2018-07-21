package fi.bitrite.android.ws;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.AppInjector;
import fi.bitrite.android.ws.repository.SettingsRepository;

public class WSAndroidApplication extends BaseWSAndroidApplication {

    @Inject SettingsRepository mSettingsRepository;

    private final HashMap<TrackerName, Tracker> mTrackers = new HashMap<>();

    public synchronized Tracker getTracker(TrackerName trackerId) {
        String PROPERTY_ID = getString(R.string.ga_property_id);

        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = (trackerId == TrackerName.APP_TRACKER)
                    ? analytics.newTracker(R.xml.app_tracker)
                    : (trackerId == TrackerName.GLOBAL_TRACKER)
                            ? analytics.newTracker(R.xml.global_tracker)
                            : analytics.newTracker(PROPERTY_ID);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

    public void onCreate() {
        super.onCreate();

        // Set automatic activity reports, per http://stackoverflow.com/a/24983778/215713
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        analytics.enableAutoActivityReports(this);
        analytics.setAppOptOut(!mSettingsRepository.canCollectStats());
    }

    @Override
    protected AppInjector inject() {
        return AppInjector.create(this);
    }


    // Google Analytics Support
    public enum TrackerName {
        APP_TRACKER,
        GLOBAL_TRACKER
    }
}
