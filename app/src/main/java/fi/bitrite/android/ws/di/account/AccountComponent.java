package fi.bitrite.android.ws.di.account;

import android.accounts.Account;

import javax.annotation.Nullable;
import javax.inject.Named;

import dagger.BindsInstance;
import dagger.Subcomponent;
import dagger.android.AndroidInjectionModule;
import fi.bitrite.android.ws.AutoMessageReloadScheduler;
import fi.bitrite.android.ws.ui.MainActivity;
import io.reactivex.disposables.CompositeDisposable;

@AccountScope
@Subcomponent(modules = {
        AccountModule.class,
        ActivitiesModule.class,
        AndroidInjectionModule.class,
        WebserviceModule.class,
})
public interface AccountComponent {
    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        Builder account(Account account);

        AccountComponent build();
    }

    /**
     * This initializes the eager instances in {@link AccountModule}.
     */
    @Nullable
    Void init();

    @Named("accountDestructor")
    CompositeDisposable getAccountDestructor();

    void inject(AutoMessageReloadScheduler.AccountHelper accountHelper);
    void inject(MainActivity.AccountHelper accountHelper);
}
