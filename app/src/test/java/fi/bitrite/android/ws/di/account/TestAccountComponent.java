package fi.bitrite.android.ws.di.account;

import android.accounts.Account;

import dagger.BindsInstance;
import dagger.Subcomponent;
import dagger.android.AndroidInjectionModule;
import fi.bitrite.android.ws.auth.AuthTest;

@AccountScope
@Subcomponent(modules = {
        AccountModule.class,
        ActivitiesModule.class,
        AndroidInjectionModule.class,
        WebserviceModule.class,
})
public interface TestAccountComponent extends AccountComponent {
    @Subcomponent.Builder
    interface Builder extends AccountComponent.Builder {
        @Override
        @BindsInstance
        Builder account(Account account);

        @Override
        TestAccountComponent build();
    }

    void inject(AuthTest authTest);
}
