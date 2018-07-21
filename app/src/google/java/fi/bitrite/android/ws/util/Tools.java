package fi.bitrite.android.ws.util;

import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import fi.bitrite.android.ws.WSAndroidApplication;

/**
 * General simple tools, mostly public methods.
 */
public class Tools extends BaseTools {

    // Send a report to Google Analytics about  category/action
    static public void gaReportException(Context context, String category, String action) {

        Tracker exceptionTracker = ((WSAndroidApplication) context.getApplicationContext())
                .getTracker(WSAndroidApplication.TrackerName.APP_TRACKER);

        exceptionTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .build()
        );
    }
}
