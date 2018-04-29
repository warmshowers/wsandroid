package fi.bitrite.android.ws;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import fi.bitrite.android.ws.repository.MessageRepository;
import fi.bitrite.android.ws.repository.SettingsRepository;
import fi.bitrite.android.ws.ui.MessageNotificationController;

public class WSAndroidService extends Service
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject MessageRepository mMessageRepository;
    @Inject MessageNotificationController mMessageNotificationController;
    @Inject SettingsRepository mSettingsRepository;

    private final Timer mTimer = new Timer();
    private ReloadMessagesTask mNextReloadTask;

    private int mMessageReloadIntervalMs;

    @Override
    public void onCreate() {
        super.onCreate();

        WSAndroidApplication.getAppComponent().inject(this);

        // Register for settings updates. That triggers an initial run of the change listener.
        mSettingsRepository.registerOnChangeListener(this);

        // Reload the messages now.
        if (mMessageReloadIntervalMs > 0) {
            new ReloadMessagesTask().run();
        }
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
        mSettingsRepository.unregisterOnChangeListener(this);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null || key.equals(mSettingsRepository.getMessageRefreshIntervalKey())) {
            mMessageReloadIntervalMs =
                    mSettingsRepository.getMessageRefreshIntervalMin() * 60 * 1000;
            scheduleNextRun();
        }
    }

    private void scheduleNextRun() {
        if (mNextReloadTask != null) {
            mNextReloadTask.cancel();
            mNextReloadTask = null;
        }

        if (mMessageReloadIntervalMs == 0) {
            // Zero means no scheduled executions.
            return;
        }

        mNextReloadTask = new ReloadMessagesTask();
        mTimer.schedule(mNextReloadTask, mMessageReloadIntervalMs);
    }

    private class ReloadMessagesTask extends TimerTask {
        @Override
        public void run() {
            mMessageRepository.reloadThreads()
                    .onErrorComplete()
                    .subscribe(WSAndroidService.this::scheduleNextRun);
        }
    }
}
