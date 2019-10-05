package fi.bitrite.android.ws.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import javax.inject.Inject;

import fi.bitrite.android.ws.WSAndroidApplication;

public class AuthenticationService extends Service {

    @Inject Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        WSAndroidApplication.getAppComponent().inject(this);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
