package fi.bitrite.android.ws.di;

import android.app.Application;
import android.content.Context;

import javax.inject.Named;

import androidx.annotation.Nullable;
import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.AutoMessageReloadScheduler;
import fi.bitrite.android.ws.api.WarmshowersWebservice;
import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.auth.Authenticator;
import fi.bitrite.android.ws.di.account.AccountComponent;
import fi.bitrite.android.ws.repository.UserRepository;

@Module(subcomponents = {AccountComponent.class})
public class AppModule {

    @Provides
    Context provideApplicationContext(Application application) {
        return application.getApplicationContext();
    }

    @Provides
    android.accounts.AccountManager provideAndroidAccountManager(Context appContext) {
        return android.accounts.AccountManager.get(appContext);
    }

    @Provides
    Authenticator provideAuthenticator(Context context,
                                       AccountManager accountManager,
                                       WarmshowersWebservice generalWebservice,
                                       UserRepository.AppUserRepository appUserRepository) {
        return new Authenticator(context, accountManager, generalWebservice, appUserRepository);
    }

    /**
     * Parameters to this method are ensured to be created eagerly (in contrast to the default lazy
     * creation).
     */
    @Provides
    @Named("eager-app")
    @Nullable
    Void provideEager(AutoMessageReloadScheduler autoMessageReloadScheduler) {
        // This eagerly builds any parameters specified and returns nothing.
        return null;
    }
}
