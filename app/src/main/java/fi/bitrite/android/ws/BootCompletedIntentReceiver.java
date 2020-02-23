package fi.bitrite.android.ws;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedIntentReceiver extends BroadcastReceiver {
    private static final String TAG = BootCompletedIntentReceiver.class.getCanonicalName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received broadcast: " + (intent != null ? intent.getAction() : "null"));
        if (intent == null) {
            return;
        }
        if (!intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        // This leads to AutoMessageReloadScheduler being (eagerly) created.
        WSAndroidApplication.getAppComponent().inject(this);
    }
}
