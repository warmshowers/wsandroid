package fi.bitrite.android.ws.di;

import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;

import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.di.account.AccountComponent;

@Module(subcomponents = {AccountComponent.class})
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
