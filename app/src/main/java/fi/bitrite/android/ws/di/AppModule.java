package fi.bitrite.android.ws.di;

import android.accounts.AccountManager;
import android.app.Application;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    @Provides
    public AccountManager providesAccountManager(Application application) {
        return AccountManager.get(application.getApplicationContext());
    }
}
