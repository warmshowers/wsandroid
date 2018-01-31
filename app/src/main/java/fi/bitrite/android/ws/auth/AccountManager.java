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
    public final static int UNKNOWN_USER_ID = 0;

    private final AuthenticationManager mAuthenticationManager;

    private final BehaviorSubject<MaybeNull<Account>> mCurrentAccount;
    private final Observable<Integer> mCurrentUserId;

    @Inject
    AccountManager(AuthenticationManager authenticationManager) {
        mAuthenticationManager = authenticationManager;

        BehaviorSubject<Account[]> rxAccounts = mAuthenticationManager.getExistingAccounts();

        Account[] accounts = rxAccounts.getValue();
        mCurrentAccount = BehaviorSubject.createDefault(new MaybeNull<>(
                // TODO(saemy): Remember the one used last time.
                accounts.length > 0 ? accounts[0] : null));
        mCurrentUserId = mCurrentAccount
                .map(maybeAccount -> maybeAccount.isNonNull()
                        ? mAuthenticationManager.getUserId(maybeAccount.data)
                        : UNKNOWN_USER_ID);

        // Removes the current account if it got removed and uses a new one if no other is used.
        rxAccounts.subscribe(newAccounts -> {
            MaybeNull<Account> current = mCurrentAccount.getValue();
            if (current.isNonNull() && !Arrays.asList(newAccounts).contains(current.data)) {
                // Our current account no longer exists.
                setCurrentAccount(newAccounts.length > 0 ? newAccounts[0] : null);
            } else if (current.isNull() && newAccounts.length > 0) {
                // We did not have any account set so far.
                setCurrentAccount(newAccounts[0]);
            }
        });
    }

    public BehaviorSubject<Account[]> getAccounts() {
        return mAuthenticationManager.getExistingAccounts();
    }

    public BehaviorSubject<MaybeNull<Account>> getCurrentAccount() {
        return mCurrentAccount;
    }
    public Observable<Integer> getCurrentUserId() {
        return mCurrentUserId;
    }

    public void setCurrentAccount(Account account) {
        mCurrentAccount.onNext(new MaybeNull<>(account));
    }
}
