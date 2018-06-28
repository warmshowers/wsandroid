package fi.bitrite.android.ws.di.account;

import android.accounts.Account;

import javax.annotation.Nullable;
import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.AutoMessageReloadScheduler;
import fi.bitrite.android.ws.api.AuthenticationController;
import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.ui.MessageNotificationController;
import io.reactivex.disposables.CompositeDisposable;

@Module
public class AccountModule {
    private final CompositeDisposable mAccountDisposable = new CompositeDisposable();

    @Provides
    @Named("accountUserId")
    int provideAccountUserId(AccountManager accountManager, Account account) {
        return accountManager.getUserId(account);
    }

    /**
     * Parameters to this method are ensured to be created eagerly (in contrast to the default lazy
     * creation).
     */
    @Provides
    @Nullable
    Void provideEager(AuthenticationController authenticationController,
                      AutoMessageReloadScheduler autoMessageReloadScheduler,
                      MessageNotificationController messageNotificationController) {
        // This eagerly builds any parameters specified and returns nothing.
        return null;
    }

    /**
     * This disposable is used to dispose all registered
     * {@link io.reactivex.disposables.Disposable}s as soon as the current account is no longer
     * available (meaning removed by the user).
     */
    @Provides
    @Named("accountDestructor")
    CompositeDisposable provideAccountDisposable() {
        return mAccountDisposable;
    }
}