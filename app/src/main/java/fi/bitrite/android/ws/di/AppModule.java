package fi.bitrite.android.ws.di;

import android.accounts.AccountManager;
import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.activity.ActivityHelper;

@Module
public class AppModule {

    @Provides
    public AccountManager providesAccountManager(Application application) {
        return AccountManager.get(application.getApplicationContext());
    }

    @Provides
    @Singleton
    public ActivityHelper provideActivityHelper() {
        return new ActivityHelper();
    }
}
