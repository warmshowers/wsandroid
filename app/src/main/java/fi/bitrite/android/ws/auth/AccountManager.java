package fi.bitrite.android.ws.auth;

import android.accounts.Account;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.util.MaybeNull;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

@Singleton
public class AccountManager {

    private final AuthenticationManager mAuthenticationManager;

    private final BehaviorSubject<MaybeNull<Account>> mCurrentAccount;
    private final Observable<Integer> mCurrentAccountId;

    @Inject
    AccountManager(AuthenticationManager authenticationManager) {
        mAuthenticationManager = authenticationManager;

        BehaviorSubject<Account[]> rxAccounts = mAuthenticationManager.getExistingAccounts();

        Account[] accounts = rxAccounts.getValue();
        mCurrentAccount = BehaviorSubject.createDefault(new MaybeNull<>(
                // TODO(saemy): Remember the one used last time.
                accounts.length > 0 ? accounts[0] : null));
        mCurrentAccountId = mCurrentAccount
                .map(accountPossiblyEmpty -> accountPossiblyEmpty.isNonNull()
                        ? mAuthenticationManager.getUserId(accountPossiblyEmpty.data)
                        : -1);

        // Removes the current account if it got removed and uses a new one if no other is used.
        rxAccounts.subscribe(newAccounts -> {
            MaybeNull<Account> currentAccount = mCurrentAccount.getValue();
            if (currentAccount.isNonNull() && !Arrays.asList(newAccounts).contains(currentAccount.data)) {
                setCurrentAccount(accounts.length > 0 ? accounts[0] : null);
            } else if (currentAccount.isNull() && accounts.length > 0) {
                setCurrentAccount(accounts[0]);
            }
        });
    }

    public BehaviorSubject<Account[]> getAccounts() {
        return mAuthenticationManager.getExistingAccounts();
    }

    public BehaviorSubject<MaybeNull<Account>> getCurrentAccount() {
        return mCurrentAccount;
    }
    public Observable<Integer> getCurrentAccountId() {
        return mCurrentAccountId;
    }

    public void setCurrentAccount(Account account) {
        mCurrentAccount.onNext(new MaybeNull<>(account));
    }
}
