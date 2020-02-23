package fi.bitrite.android.ws;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import io.reactivex.disposables.Disposable;

public class AutoMessageReloadService extends Service {
    private final static String TAG = AutoMessageReloadService.class.getCanonicalName();
    private final static String KEY_MESSAGE_RELOAD_INTERVAL_MS = "messageReloadInterval";

    @Inject AutoMessageReloadScheduler mAutoMessageReloadScheduler;

    private long mMessageReloadIntervalMs = -1;
    private Timer mTimer;
    private ReloadMessagesTask mReloadTask;

    /**
     * Starts the service to periodically reload the message threads.
     */
    public static void reschedule(Context context, long messageReloadIntervalMs) {
        Log.d(TAG, String.format("reschedule: %dms", messageReloadIntervalMs));
        Intent serviceIntent = new Intent(context, AutoMessageReloadService.class);
        serviceIntent.putExtra(KEY_MESSAGE_RELOAD_INTERVAL_MS, messageReloadIntervalMs);
        if (messageReloadIntervalMs > 0) {
            context.startService(serviceIntent);
        } else {
            context.stopService(serviceIntent);
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "create");
        super.onCreate();

        WSAndroidApplication.getAppComponent().inject(this);

        mTimer = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // This method is fired every time we do a startService call in reschedule().
        long messageReloadIntervalMs = intent.getLongExtra(KEY_MESSAGE_RELOAD_INTERVAL_MS, 0);
        Log.d(TAG, String.format("Start: %dms", messageReloadIntervalMs));

        if (messageReloadIntervalMs != mMessageReloadIntervalMs) {
            // The service is already started but reschedule() was called once more.
            Log.d(TAG, "Scheduling message reload job");
            mMessageReloadIntervalMs = messageReloadIntervalMs;
            scheduleReloadTask(messageReloadIntervalMs);
        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "destroy");
        mTimer.cancel();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void scheduleReloadTask(long messageReloadIntervalMs) {
        // Cancel any scheduled task.
        if (mReloadTask != null) {
            Log.d(TAG, "Cancelling currently scheduled task");
            mReloadTask.cancel();
            mReloadTask = null;
        }

        // Zero means no scheduled executions.
        if (messageReloadIntervalMs == 0) {
            Log.d(TAG, "Disabling message auto-reloading");
            return;
        }

        // Schedule the task now and then every messageReloadIntervalMs.
        mReloadTask = new ReloadMessagesTask();
        Log.d(TAG, String.format("Scheduled to run at intervals of %dms", messageReloadIntervalMs));
        mTimer.schedule(mReloadTask, 0, messageReloadIntervalMs);
    }

    class ReloadMessagesTask extends TimerTask {
        private Disposable mDisposable;

        @Override
        public void run() {
            dispose();
            Log.d(TAG, "Auto-reloading messages");
            mDisposable = mAutoMessageReloadScheduler.reloadMessagesInAllAccounts()
                    .onErrorComplete()
                    .subscribe(() -> Log.d(TAG, "Auto-reloading messages completed."));
        }

        @Override
        public boolean cancel() {
            dispose();
            return super.cancel();
        }

        private void dispose() {
            if (mDisposable != null) {
                mDisposable.dispose();
                mDisposable = null;
            }
        }
    }
}
