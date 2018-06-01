package fi.bitrite.android.ws.di.account;

import android.accounts.Account;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.di.AppComponent;
import fi.bitrite.android.ws.di.AppScope;

@AppScope // Correct!
public class AccountComponentManager {
    private final Map<String, AccountComponent> mAccountCompontents = new HashMap<>();

    private final AppComponent mAppComponent;
    private Account mCurrentAccount;

    @Inject
    AccountComponentManager(AppComponent appComponent, AccountManager accountManager) {
        mAppComponent = appComponent;

        accountManager.getCurrentAccount()
                .subscribe(maybeAccount -> {
                    Account[] accounts = accountManager.getAccounts().getValue();
                    if (accounts != null && !Arrays.asList(accounts).contains(maybeAccount.data)) {
                        // The previous account no longer is in the list -> it got removed.
                        remove(mCurrentAccount);
                    }
                    mCurrentAccount = maybeAccount.data;
                });
    }

    public AccountComponent get(Account account) {
        AccountComponent component = mAccountCompontents.get(account.name);
        if (component == null) {
            component = mAppComponent.getAccountComponentBuilder()
                    .account(account)
                    .build();
            component.init();
            mAccountCompontents.put(account.name, component);
        }
        return component;
    }

    @Nullable
    public AccountComponent getCurrent() {
        return mCurrentAccount != null
                ? get(mCurrentAccount)
                : null;
    }

    public Collection<AccountComponent> getAll() {
        return mAccountCompontents.values();
    }

    private void remove(Account account) {
        if (account == null) {
            return;
        }
        AccountComponent component = mAccountCompontents.remove(account.name);
        if (component != null) {
            component.getAccountDestructor().dispose();
        }
    }
}
