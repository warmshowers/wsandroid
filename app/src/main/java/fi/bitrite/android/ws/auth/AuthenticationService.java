package fi.bitrite.android.ws.auth;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import javax.inject.Inject;

import fi.bitrite.android.ws.WSAndroidApplication;

public class AuthenticationService extends Service {

    @Inject AccountManager accountManager;

    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        WSAndroidApplication.getAppComponent().inject(this);

        mAuthenticator = new Authenticator(this, accountManager);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
