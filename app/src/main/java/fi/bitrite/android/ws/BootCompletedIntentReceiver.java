package fi.bitrite.android.ws;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class BootCompletedIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TextUtils.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        // This leads to AutoMessageReloadScheduler being created.
        WSAndroidApplication.getAppComponent().inject(this);
    }
}
