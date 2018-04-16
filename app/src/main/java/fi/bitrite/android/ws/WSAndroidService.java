package fi.bitrite.android.ws;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import fi.bitrite.android.ws.repository.MessageRepository;
import fi.bitrite.android.ws.ui.MessageNotificationController;

public class WSAndroidService extends Service {

    private final static int RELOAD_MESSAGES_EVERY_MS = 5 * 60 * 1000;

    @Inject MessageRepository mMessageRepository;
    @Inject MessageNotificationController mMessageNotificationController;

    private final Timer mTimer = new Timer();

    @Override
    public void onCreate() {
        WSAndroidApplication.getAppComponent().inject(this);

        // Reload the messages now.
        new ReloadMessagesTask().run();
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class ReloadMessagesTask extends TimerTask {
        @Override
        public void run() {
            mMessageRepository.reloadThreads()
                    .onErrorComplete()
                    .subscribe(() -> mTimer.schedule(new ReloadMessagesTask(),
                                                     RELOAD_MESSAGES_EVERY_MS));
        }
    };
}
