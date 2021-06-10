package fi.bitrite.android.ws;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import fi.bitrite.android.ws.ui.ActivityMovingOut;

public class NotificationWorker extends Worker {
    private final static String TAG = NotificationScheduler.class.getCanonicalName();

    private final static int NOTIFICATION_ID = 1;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /**
     * Shows a notification in case the current time is later than the minimum next notification
     * time stamp.
     * This function is idempotent in case a notification is already shown to the user.
     */
    @Override
    @NonNull
    public Result doWork() {
        Context context = getApplicationContext();

        Settings settings = new Settings(context);
        long nextNotificationMs = settings.getNextNotificationMs();
        if (System.currentTimeMillis() < nextNotificationMs) {
            Log.d(TAG, String.format(
                    "Notification cool down period not yet reached (current: %d, min: %d)",
                    System.currentTimeMillis(),
                    nextNotificationMs));
            return Result.success();
        }

        showNotification(context);
        Log.d(TAG, "Notification shown");

        return Result.success();
    }

    /**
     * Shows the notification
     * @param context
     */
    public static void showNotification(Context context) {
        Intent intent = new Intent(context, ActivityMovingOut.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Intent snoozeIntent = new Intent(context, BroadcastReceiver.class);
        snoozeIntent.setAction(BroadcastReceiver.ACTION_SNOOZE_NOTIFICATION);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, 0, snoozeIntent, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        WSAndroidApplication.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_bicycle_white_24dp)
                        .setContentTitle(context.getString(R.string.moving_out_notification_title))
                        .setContentText(context.getString(R.string.moving_out_notification_text_short))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(context.getString(R.string.moving_out_notification_text_short) + ".\n"
                                         + context.getString(R.string.moving_out_notification_text_long)))
                        .setOnlyAlertOnce(true)
                        .setContentIntent(clickPendingIntent)
                        .setDeleteIntent(snoozePendingIntent)
                        .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * Removes the currently shown notification.
     * @param context
     */
    public static void removeNotification(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
        Log.d(TAG, "Removed notification");
    }
}
