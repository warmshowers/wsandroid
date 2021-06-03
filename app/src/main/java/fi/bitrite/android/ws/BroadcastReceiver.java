package fi.bitrite.android.ws;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

public class BroadcastReceiver extends android.content.BroadcastReceiver {
    private static final String TAG = BroadcastReceiver.class.getCanonicalName();

    public static final String ACTION_CANCEL_NOTIFICATION = "fi.bitrite.android.ws.cancelNotification";
    public static final String ACTION_SNOOZE_NOTIFICATION = "fi.bitrite.android.ws.snoozeNotification";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received broadcast: " + (intent != null ? intent.getAction() : "null"));
        if (intent == null) {
            return;
        }

        // There is nothing to do for the following actions as the notification scheduler is
        // implicitly set up in WSAndroidApplication before reaching here:
        // - Intent.BOOT_COMPLETED
        // - Intent.QUICKBOOT_POWERON
        // - Intent.MY_PACKAGE_REPLACED

        String action = Objects.requireNonNull(intent.getAction());
        if (action.equalsIgnoreCase(ACTION_SNOOZE_NOTIFICATION)) {
            NotificationScheduler notificationScheduler = new NotificationScheduler(context);
            notificationScheduler.snoozeNotification();
        } else if (action.equalsIgnoreCase(ACTION_CANCEL_NOTIFICATION)) {
            NotificationScheduler notificationScheduler = new NotificationScheduler(context);
            notificationScheduler.disableNotifications();
        }
    }
}
