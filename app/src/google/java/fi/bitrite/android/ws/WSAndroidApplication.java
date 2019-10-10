package fi.bitrite.android.ws;

import android.os.Build;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.util.HashMap;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.AppInjector;
import fi.bitrite.android.ws.repository.SettingsRepository;

public class WSAndroidApplication extends BaseWSAndroidApplication {

    @Inject SettingsRepository mSettingsRepository;

    private final HashMap<TrackerName, Tracker> mTrackers = new HashMap<>();

    public synchronized Tracker getTracker(TrackerName trackerId) {
        final String TRACKING_ID = getString(R.string.ga_tracking_id);

        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = (trackerId == TrackerName.APP_TRACKER)
                    ? analytics.newTracker(R.xml.app_tracker)
                    : (trackerId == TrackerName.GLOBAL_TRACKER)
                            ? analytics.newTracker(R.xml.global_tracker)
                            : analytics.newTracker(TRACKING_ID);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT <= 20) {
            // Ensure that the latest security provider is installed on <= Android 4.4 devices to
            // enable TLS1.2.
            try {
                ProviderInstaller.installIfNeeded(this);
            } catch (GooglePlayServicesRepairableException e) {
                // Indicates that Google Play services is out of date, disabled, etc.
                // Prompt the user to install/update/enable Google Play services.
                GoogleApiAvailability.getInstance()
                        .showErrorNotification(this, e.getConnectionStatusCode());
            } catch (GooglePlayServicesNotAvailableException e) {
                // Ignore.
            }
        }


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
