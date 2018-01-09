package fi.bitrite.android.ws.di;

import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
class AppModule {

    @Provides
    Context provideApplicationContext(Application application) {
        return application.getApplicationContext();
    }

    @Provides
    AccountManager provideAccountManager(Context appContext) {
        return AccountManager.get(appContext);
    }
}
