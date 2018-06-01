package fi.bitrite.android.ws.di;

import android.app.Application;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import fi.bitrite.android.ws.AutoMessageReloadJobService;
import fi.bitrite.android.ws.AutoMessageReloadService;
import fi.bitrite.android.ws.BootCompletedIntentReceiver;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.auth.AuthenticationService;
import fi.bitrite.android.ws.di.account.AccountComponent;

@AppScope
@Component(modules = {
        ActivitiesModule.class,
        AndroidInjectionModule.class,
        AppModule.class,
        WebserviceModule.class,
})
public interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application application);

        AppComponent build();
    }

    AccountComponent.Builder getAccountComponentBuilder();

    void inject(AuthenticationService service);
    void inject(AutoMessageReloadJobService autoMessageReloadJobService);
    void inject(AutoMessageReloadService autoMessageReloadService);
    void inject(BootCompletedIntentReceiver bootCompletedIntentReceiver);
    void inject(WSAndroidApplication wsApp);
}
