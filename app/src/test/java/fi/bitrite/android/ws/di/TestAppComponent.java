package fi.bitrite.android.ws.di;

import android.app.Application;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.di.account.AccountComponentManager;
import fi.bitrite.android.ws.di.account.TestAccountComponent;
import fi.bitrite.android.ws.util.GlideDataSaverModeTest;

@AppScope
@Component(modules = {
        ActivitiesModule.class,
        AndroidInjectionModule.class,
        AppModule.class,
        MockWebserviceModule.class,
})
public interface TestAppComponent extends AppComponent {
    @Component.Builder
    interface Builder extends AppComponent.Builder {
        @Override
        @BindsInstance
        Builder application(Application application);

        @Override
        TestAppComponent build();
    }

    /**
     * This results in `AccountComponentManager` using the test's `AccountComponent.Buider`.
     */
    @Override
    TestAccountComponent.Builder getAccountComponentBuilder();

    AccountComponentManager getAccountComponentManager();
    AccountManager getAccountManager();

    void inject(GlideDataSaverModeTest test);
}
