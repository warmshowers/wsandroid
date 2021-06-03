package fi.bitrite.android.ws;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {
    private final SharedPreferences mSharedPreferences;

    private final static String KEY_NEXT_NOTIFICATION = "nextNotification";
    private final static String KEY_NOTIFICATION_SNOOZE_COUNT = "notificationSnoozeCount";

    public Settings(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Sets the earliest timestamp to show the next notification.
     * @param nextNotificationMs
     */
    public void setNextNotificationMs(long nextNotificationMs) {
        mSharedPreferences
                .edit()
                .putLong(KEY_NEXT_NOTIFICATION, nextNotificationMs)
                .apply();
    }

    /**
     * Returns the earliest timestamp to show the next notification.
     * @return
     */
    public long getNextNotificationMs() {
        return mSharedPreferences.getLong(KEY_NEXT_NOTIFICATION, 0);
    }

    public int incAndGetNotificationSnoozeCount() {
        int notificationSnoozeCount =
                mSharedPreferences.getInt(KEY_NOTIFICATION_SNOOZE_COUNT, 0) + 1;
        mSharedPreferences
                .edit()
                .putInt(KEY_NOTIFICATION_SNOOZE_COUNT, notificationSnoozeCount)
                .apply();
        return notificationSnoozeCount;
    }
}
