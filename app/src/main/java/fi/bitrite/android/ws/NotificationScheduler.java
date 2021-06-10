package fi.bitrite.android.ws;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class NotificationScheduler {
    private final static String TAG = NotificationScheduler.class.getCanonicalName();
    private final static String UNIQUE_WORK_NAME = "notification";

    private final Context mContext;
    private final Settings mSettings;
    private final WorkManager mWorkManager;

    public NotificationScheduler(Context context) {
        mContext = context;
        mSettings = new Settings(context);
        mWorkManager = WorkManager.getInstance(context);
    }

    /**
     * Register a work request with Android's work manager that checks every two hours whether to
     * show a notification to the user. That work item is persisted even after application shutdown,
     * device restart, ...
     * After notifications got disabled (see {@link #disableNotifications()}) this function no
     * longer registers any work request and just returns.
     */
    public void schedule() {
        if (mSettings.getNextNotificationMs() == Long.MAX_VALUE) {
            // The notifications got disabled.
            Log.d(TAG, "Not scheduling any work as notifications are disabled");
            return;
        }

        PeriodicWorkRequest notificationWorkRequest =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 2, TimeUnit.HOURS)
                        .setInitialDelay(3, TimeUnit.MINUTES)
                        .build();

        mWorkManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                notificationWorkRequest);
        Log.d(TAG, "Scheduled notification worker");
    }

    /**
     * Removes the currently shown notification and re-schedules it, applying a linear backoff
     * strategy for the time between two notifications.
     */
    public void snoozeNotification() {
        NotificationWorker.removeNotification(mContext);

        // Have a cool down period of n ticks between showing a notification, where n is the number
        // of times the notification got snoozed.
        // We delay for 21 hours per tick to make the notification not appear at the same time of
        // the day every time.
        int delayTicks = mSettings.incAndGetNotificationSnoozeCount();
        final int msPerHour = 60 * 60 * 1000;
        int delayMs = delayTicks * 21 * msPerHour;

        final int maxDelayMs = 7 * 24 * msPerHour; // 1 week
        if (delayMs > maxDelayMs) {
            delayMs = maxDelayMs;
        }

        mSettings.setNextNotificationMs(System.currentTimeMillis() + delayMs);
        Log.d(TAG, String.format("Snoozed notification for %.2f days", delayMs / msPerHour / 24.0));
    }

    /**
     * Disables the notification. No more notifications are shown after calling this function.
     */
    public void disableNotifications() {
        NotificationWorker.removeNotification(mContext);

        mSettings.setNextNotificationMs(Long.MAX_VALUE);
        mWorkManager.cancelUniqueWork(UNIQUE_WORK_NAME);
        Log.d(TAG, "Disabled notifications");
    }
}

