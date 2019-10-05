package fi.bitrite.android.ws;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;

public class AutoMessageReloadService extends Service {
    private final static String TAG = AutoMessageReloadService.class.getCanonicalName();

    @Inject AutoMessageReloadScheduler mAutoMessageReloadScheduler;

    private Timer mTimer;
    private ReloadMessagesTask mReloadTask;

    /**
     * Starts the service to periodically reload the message threads.
     */
    public static void reschedule(Context context, long messageReloadInterval) {
        Intent serviceIntent = new Intent(context, AutoMessageReloadService.class);
        if (messageReloadInterval > 0) {
            context.startService(serviceIntent);
        } else {
            context.stopService(serviceIntent);
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Auto-message reload service: created.");
        super.onCreate();

        WSAndroidApplication.getAppComponent().inject(this);

        // Reload the messages now.
        mTimer = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // This method is fired every time we do a startService call in reschedule().
        Log.d(TAG, "Auto-message reload service: onStartCommand trigger.");
        scheduleReloadTask();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Auto-message reload service: destroyed.");
        mTimer.cancel();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void scheduleReloadTask() {
        // Cancel any scheduled task.
        if (mReloadTask != null) {
            Log.d(TAG, "Cancelling currently scheduled task");
            mReloadTask.cancel();
            mReloadTask = null;
        }

        final long messageReloadIntervalMs =
                mAutoMessageReloadScheduler.getMessageReloadIntervalMs();

        // Zero means no scheduled executions.
        if (messageReloadIntervalMs == 0) {
            Log.d(TAG, "Disabling message auto-reloading");
            return;
        }

        // Schedule the tasks.
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
