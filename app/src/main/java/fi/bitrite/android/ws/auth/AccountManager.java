package fi.bitrite.android.ws.auth;

import android.accounts.Account;
import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.di.AppScope;
import fi.bitrite.android.ws.ui.MainActivity;
import fi.bitrite.android.ws.util.MaybeNull;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

@AppScope
public class AccountManager {
    public final static int UNKNOWN_USER_ID = 0;

    private final static String AUTH_TOKEN_TYPE = "FULL_ACCESS";

    private final static String KEY_USER_ID = "user_id";
    private final static String KEY_CSRF_TOKEN = "csrf_token";

    private final android.accounts.AccountManager mAndroidAccountManager;

    private final BehaviorSubject<Account[]> mAccounts = BehaviorSubject.create();
    private final BehaviorSubject<MaybeNull<Account>> mCurrentAccount = BehaviorSubject.create();
    private final Observable<Integer> mCurrentUserId;

    private Activity mMainActivity = null;
    private EventuallyCreateOrAuth mEventuallyCreateOrAuth = null;

    @Inject
    AccountManager(android.accounts.AccountManager androidAccountManager) {
        mAndroidAccountManager = androidAccountManager;

        mAndroidAccountManager.addOnAccountsUpdatedListener(
                accounts_unused -> handleAccountUpdate(), null, false);
        handleAccountUpdate();

        mCurrentUserId = mCurrentAccount
                .map(maybeAccount -> maybeAccount.isNonNull()
                        ? getUserId(maybeAccount.data)
                        : UNKNOWN_USER_ID);
    }

    /**
     * Returns the accounts that are stored in the Android account system.
     *
     * @return The accounts
     */
    public BehaviorSubject<Account[]> getAccounts() {
        return mAccounts;
    }

    public BehaviorSubject<MaybeNull<Account>> getCurrentAccount() {
        return mCurrentAccount;
    }
    public void setCurrentAccount(Account account) {
        mCurrentAccount.onNext(new MaybeNull<>(account));
    }
    public Observable<Integer> getCurrentUserId() {
        return mCurrentUserId;
    }

    private void handleAccountUpdate() {
        Account[] newAccounts;
        synchronized (this) {
            // Synchronize the access with the code in {@link #updateOrCreateAccount()} that might
            // trigger this handler before any options of the new account are set.
            newAccounts = mAndroidAccountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE);
        }

        // Ensure that we do not consider any accounts without an account userId.
        List<Account> newAccountsFiltered = new LinkedList<>();
        for (Account account : newAccounts) {
            int accountUserId = getUserId(account);
            if (accountUserId == UNKNOWN_USER_ID) {
                // Ignore this account.
                Log.w(WSAndroidApplication.TAG,
                        "Ignoring account due to not having a userId: " + account.name);
                continue;
            }
            newAccountsFiltered.add(account);
        }
        newAccounts = newAccountsFiltered.toArray(new Account[newAccountsFiltered.size()]);

        if (Arrays.equals(mAccounts.getValue(), newAccounts)) { // They need to be in the same order!
            // Nothing changed.
            return;
        }
        mAccounts.onNext(newAccounts);

        // Removes the current account if it got removed and uses a new one if no other is used.
        MaybeNull<Account> current = mCurrentAccount.getValue();
        if (current == null) {
            // Initial value.
            // TODO(saemy): Remember the one used last time.
            setCurrentAccount(newAccounts.length > 0 ? newAccounts[0] : null);
        } else if (current.isNonNull() && !Arrays.asList(newAccounts).contains(current.data)) {
            // Our current account no longer exists.
            setCurrentAccount(newAccounts.length > 0 ? newAccounts[0] : null);
        } else if (current.isNull() && newAccounts.length > 0) {
            // We did not have any account set so far.
            setCurrentAccount(newAccounts[0]);
        }

        if (newAccounts.length == 0) {
            // There are no accounts. Lets ask the user to create one.
            createNewAccount().subscribe(a -> {}, e -> {
                // TODO(saemy): Error handling.
            });
        } else {
            // We have an account and therefore no longer need to create a new one.
            dismissEventuallyCreateOrAuth();
        }
    }

    /**
     * Sets the main activity of this app. This is used to show the login screen in case an account
     * needs to be created or a re-login is necessary.
     * @param mainActivity Can be null in case the main activity is destroyed.
     */
    public void setMainActivity(MainActivity mainActivity) {
        mMainActivity = mainActivity;
        if (mEventuallyCreateOrAuth != null) {
            // The main activity was not around when we wanted to show the account creation screen.
            // Do it now.
            final Intent intent = mEventuallyCreateOrAuth.intent;
            final MaybeObserver<? super Bundle> observer = mEventuallyCreateOrAuth.observer;
            mEventuallyCreateOrAuth = null;
            startActivityForResult(intent, observer);
        }
    }

    /**
     * Creates a new account, the {@link fi.bitrite.android.ws.ui.AuthenticatorActivity} is
     * shown to the user.
     *
     * @return The new account as soon as it becomes available.
     */
    public Single<Account> createNewAccount() {
        return Single.<Account>create(emitter -> {
            Bundle options = new Bundle();

            AccountManagerCallback<Bundle> accountManagerCallback = accountManagerFuture -> {
                try {
                    Bundle result = accountManagerFuture.getResult();

                    handleIntentInBundle(result)
                            .subscribe(result2 -> {
                                final String name = result2.getString(
                                        android.accounts.AccountManager.KEY_ACCOUNT_NAME);
                                final String type = result2.getString(
                                        android.accounts.AccountManager.KEY_ACCOUNT_TYPE);

                                // TODO(saemy): Mark this account as the active one.

                                emitter.onSuccess(new Account(name, type));
                            }, emitter::onError);
                } catch (Exception e) {
                    emitter.onError(e);
                }
            };

            // Asks the user to create the new account. This is done by showing the login page.
            mAndroidAccountManager.addAccount(
                    BuildConfig.ACCOUNT_TYPE, AUTH_TOKEN_TYPE, null, options, mMainActivity,
                    accountManagerCallback, null);
        }).toObservable().firstOrError();
    }

    /**
     * Peeks the authToken of the given account from the Android account service. Null is returned,
     * if the token is not available and no further action is taken.
     *
     * @param account The account to get the authToken for.
     * @return The authToken or null if not available
     */
    @Nullable
    public AuthToken peekAuthToken(@NonNull Account account) {
        String authTokenStr = mAndroidAccountManager.peekAuthToken(account, AUTH_TOKEN_TYPE);
        return authTokenStr == null
                ? null
                : AuthToken.fromString(authTokenStr);
    }

    @Nullable
    public String getPassword(@NonNull Account account) {
        return mAndroidAccountManager.getPassword(account);
    }

    /**
     * Gets the authToken of the given account from the Android account service. If the token is not
     * available, the {@link fi.bitrite.android.ws.ui.AuthenticatorActivity} is shown to the user
     * s.t. they can re-login.
     *
     * @param account The account to get the authToken for.
     * @return The authToken as soon as it becomes available.
     */
    public Single<AuthToken> getAuthToken(@NonNull Account account) {
        return Single.create(emitter -> {
            AccountManagerCallback<Bundle> accountManagerCallback = tokenFuture -> {
                try {
                    Bundle result = tokenFuture.getResult();

                    Disposable unused = handleIntentInBundle(result)
                            .subscribe(result2 -> {
                                String authTokenStr = result2.getString(
                                        android.accounts.AccountManager.KEY_AUTHTOKEN);
                                AuthToken authToken = AuthToken.fromString(authTokenStr);

                                emitter.onSuccess(authToken);
                            }, emitter::onError);
                } catch (Exception e) {
                    emitter.onError(e);
                }
            };

            if (mMainActivity != null) {
                mAndroidAccountManager.getAuthToken(
                        account, AUTH_TOKEN_TYPE, null, mMainActivity, accountManagerCallback,
                        null);
            } else {
                mAndroidAccountManager.getAuthToken(
                        account, AUTH_TOKEN_TYPE, null, true, accountManagerCallback, null);
            }
        });
    }

    /**
     * Calls the intent that is stored in the given bundle. If no main activity is started yet, the
     * intent is saved for later usage. If no intent is saved in the bundle nothing is done.
     *
     * @return
     *     The single that is triggered as soon as the final bundle is available. That is the
     *     one given in case no intent is in it or the one that is eventually returned from the
     *     started activity.
     */
    private Maybe<Bundle> handleIntentInBundle(Bundle result) {
        if (!result.containsKey(android.accounts.AccountManager.KEY_INTENT)) {
            return Maybe.just(result);
        }

        return new Maybe<Bundle>() {
            @Override
            protected void subscribeActual(MaybeObserver<? super Bundle> observer) {
                Intent intent = result.getParcelable(android.accounts.AccountManager.KEY_INTENT);
                startActivityForResult(intent, observer);
            }
        };
    }

    private void startActivityForResult(Intent intent, MaybeObserver<? super Bundle> observer) {
        MainActivity mainActivity = (MainActivity) mMainActivity;
        if (mainActivity != null) {
            mainActivity.startActivityForResultRx(intent)
                    .subscribe(intent2 -> {
                                if (intent2 != null && intent2.getExtras() != null) {
                                    observer.onSuccess(intent2.getExtras());
                                } else {
                                    observer.onComplete();
                                }
                            }, observer::onError, observer::onComplete);
        } else {
            dismissEventuallyCreateOrAuth();
            mEventuallyCreateOrAuth = new EventuallyCreateOrAuth(intent, observer);
        }
    }

    private void dismissEventuallyCreateOrAuth() {
        if (mEventuallyCreateOrAuth != null) {
            mEventuallyCreateOrAuth.observer.onComplete();
            mEventuallyCreateOrAuth = null;
        }
    }

    /**
     * Creates a new account or updates an existing one with the given authData and userId.
     * The given password is stored along the account.
     */
    @VisibleForTesting
    void updateOrCreateAccount(AuthData authData, int userId, @Nullable String password) {
        // (@link AccountManager#addAccountExplicitly} triggers notifications which in
        // turn try to access e.g. the userId of that account. We therefore synchronize
        // access to the AccountManager to avoid that race.
        synchronized (this) {
            boolean isExistingAccount =
                    Arrays.asList(mAndroidAccountManager.getAccountsByType(
                            BuildConfig.ACCOUNT_TYPE))
                            .contains(authData.account);
            if (!isExistingAccount) {
                // This explicitly does not save any password as it is stored in
                // plaintext on the device. On rooted devices this is an issue! Instead,
                // the auth token is stored along the account to avoid re-logins.
                mAndroidAccountManager.addAccountExplicitly(authData.account, password, null);
            } else {
                mAndroidAccountManager.setPassword(authData.account, password);
            }

            mAndroidAccountManager.setUserData(
                    authData.account, KEY_USER_ID, Integer.toString(userId));
            mAndroidAccountManager.setUserData(
                    authData.account, KEY_CSRF_TOKEN, authData.csrfToken);
            mAndroidAccountManager.setAuthToken(
                    authData.account, AUTH_TOKEN_TYPE, authData.authToken.toString());

            if (isExistingAccount) {
                // The account might have been previously filtered due to a missing
                // userId. When the data becomes available no update is triggered as the
                // account list does not change. Therefore, we trigger the update
                // manually.
                handleAccountUpdate();
            }
        }
    }

    public int getUserId(@NonNull Account account) {
        // Migration from version <2.0.0.
        String oldUserIdStr = mAndroidAccountManager.getUserData(account,"userid");
        if (oldUserIdStr != null) {
            mAndroidAccountManager.setUserData(account, KEY_USER_ID, oldUserIdStr);
            mAndroidAccountManager.setUserData(account, "userid", null);
        }

        String userIdStr = mAndroidAccountManager.getUserData(account, KEY_USER_ID);
        return userIdStr != null
                ? Integer.parseInt(userIdStr)
                : fi.bitrite.android.ws.auth.AccountManager.UNKNOWN_USER_ID;
    }

    public String getCsrfToken(@NonNull Account account) {
        return mAndroidAccountManager.getUserData(account, KEY_CSRF_TOKEN);
    }

    /**
     * Can be called if we get notified about a changed CSRF token.
     * The new token is stored along the user.
     *
     * @param account The account to store the new token along with.
     * @param csrfToken The new token.
     */
    public void updateCsrfToken(@NonNull Account account, @NonNull String csrfToken) {
        mAndroidAccountManager.setUserData(account, KEY_CSRF_TOKEN, csrfToken);
    }

    public void invalidateAuthToken(@NonNull AuthToken authToken) {
        mAndroidAccountManager.invalidateAuthToken(BuildConfig.ACCOUNT_TYPE,
                authToken.toString());
    }

    class EventuallyCreateOrAuth {
        final Intent intent;
        final MaybeObserver<? super Bundle> observer;

        EventuallyCreateOrAuth(Intent intent, MaybeObserver<? super Bundle> observer) {
            this.intent = intent;
            this.observer = observer;
        }
    }
}
