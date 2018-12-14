package fi.bitrite.android.ws.di;

import android.app.Application;
import android.content.Context;

import dagger.Module;
import dagger.Provides;
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
}
