package fi.bitrite.android.ws;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

public class WSAndroidApplication extends Application {
    private final static String TAG = WSAndroidApplication.class.getCanonicalName();

    public final static String NOTIFICATION_CHANNEL_ID = "notifications";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Application create");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Newer Android versions require a notification channel for notifications to be shown.
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Start scheduling notifications.
        new NotificationScheduler(getApplicationContext()).schedule();
    }
}
