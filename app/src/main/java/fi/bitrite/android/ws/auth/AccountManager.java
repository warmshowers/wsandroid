package fi.bitrite.android.ws.auth;

import android.accounts.Account;
import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;
import javax.inject.Inject;

import fi.bitrite.android.ws.api.WarmshowersWebservice;
import fi.bitrite.android.ws.api.response.LoginResponse;
import fi.bitrite.android.ws.di.AppScope;
import fi.bitrite.android.ws.ui.MainActivity;
import fi.bitrite.android.ws.util.MaybeNull;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import retrofit2.Response;

@AppScope
public class AccountManager {
    public final static int UNKNOWN_USER_ID = 0;

    private final static String ACCOUNT_TYPE = "org.warmshowers";
    private final static String AUTH_TOKEN_TYPE = "FULL_ACCESS";

    private final static String KEY_USER_ID = "user_id";
    private final static String KEY_CSRF_TOKEN = "csrf_token";

    private final WarmshowersWebservice mGeneralWebservice;
    private final android.accounts.AccountManager mAndroidAccountManager;
    private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();

    private final BehaviorSubject<Account[]> mAccounts;
    private final BehaviorSubject<MaybeNull<Account>> mCurrentAccount;
    private final Observable<Integer> mCurrentUserId;

    private Activity mMainActivity = null;
    private EventuallyCreateOrAuth mEventuallyCreateOrAuth = null;

    @Inject
    AccountManager(WarmshowersWebservice generalWebservice,
                   android.accounts.AccountManager androidAccountManager) {
        mGeneralWebservice = generalWebservice;
        mAndroidAccountManager = androidAccountManager;

        Account[] accounts = mAndroidAccountManager.getAccountsByType(ACCOUNT_TYPE);
        mAccounts = BehaviorSubject.createDefault(accounts);
        mAndroidAccountManager.addOnAccountsUpdatedListener(accounts_unused -> {
            Account[] newAccounts = mAndroidAccountManager.getAccountsByType(ACCOUNT_TYPE);
            if (!Arrays.equals(mAccounts.getValue(),
                    newAccounts)) { // They need to be in the same order!
                mAccounts.onNext(newAccounts);
            }
        }, null, false);


        mCurrentAccount = BehaviorSubject.createDefault(new MaybeNull<>(
                // TODO(saemy): Remember the one used last time.
                accounts.length > 0 ? accounts[0] : null));

        // Removes the current account if it got removed and uses a new one if no other is used.
        mAccounts.subscribe(newAccounts -> {
            MaybeNull<Account> current = mCurrentAccount.getValue();
            if (current.isNonNull() && !Arrays.asList(newAccounts).contains(current.data)) {
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
        });

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
                    ACCOUNT_TYPE, AUTH_TOKEN_TYPE, null, options, mMainActivity,
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
        return executeWithReadLock(v -> {
            String authTokenStr = mAndroidAccountManager.peekAuthToken(account, AUTH_TOKEN_TYPE);
            return authTokenStr == null
                    ? null
                    : AuthToken.fromString(authTokenStr);
        });
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

                    handleIntentInBundle(result)
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

            executeWithReadLock(v -> {
                if (mMainActivity != null) {
                    mAndroidAccountManager.getAuthToken(
                            account, AUTH_TOKEN_TYPE, null, mMainActivity, accountManagerCallback,
                            null);
                } else {
                    mAndroidAccountManager.getAuthToken(
                            account, AUTH_TOKEN_TYPE, null, true, accountManagerCallback, null);
                }
                return null;
            });
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

    private void startActivityForResult(Intent intent, MaybeObserver<? super Bundle> observer){
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
     * Tries to log the given user in. On success, the account information is put into the Android
     * account service. Any account with the same username is updated or, if none found, a new one
     * is created.
     *
     * @return The login result
     */
    public Observable<LoginResult> login(String username, String password) {
        return mGeneralWebservice.login(username, password)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (!response.isSuccessful()) {
                        return new LoginResult(response);
                    }
                    LoginResponse loginResponse = response.body();

                    Account account = new Account(username, ACCOUNT_TYPE);

                    // (@link AccountManager#addAccountExplicitly} triggers notifications which in
                    // turn try to access e.g. the userId of that account. We therefore synchronize
                    // access to the AccountManager to avoid that race.
                    return executeWithWriteLock(v -> {
                        boolean isExistingAccount =
                                Arrays.asList(mAccounts.getValue()).contains(account);
                        if (!isExistingAccount) {
                            // This explicitly does not save any password as it is stored in
                            // plaintext on the device. On rooted devices this is an issue! Instead,
                            // the auth token is stored along the account to avoid re-logins.
                            mAndroidAccountManager.addAccountExplicitly(account, null, null);

                            // Sets the user id.
                            int userId = loginResponse.user.id;
                            mAndroidAccountManager.setUserData(
                                    account, KEY_USER_ID, Integer.toString(userId));
                        }

                        // Updates the CSRF token.
                        String csrfToken = loginResponse.csrfToken;
                        mAndroidAccountManager.setUserData(account, KEY_CSRF_TOKEN, csrfToken);

                        // Fetches the auth token from the login response.
                        AuthToken authToken =
                                new AuthToken(loginResponse.sessionName, loginResponse.sessionId);
                        mAndroidAccountManager.setAuthToken(
                                account, AUTH_TOKEN_TYPE, authToken.toString());

                        // Fires the callback.
                        AuthData authData = new AuthData(account, authToken, csrfToken);
                        return new LoginResult(response, authData);
                    });
                });
    }

    public int getUserId(@NonNull Account account) {
        return executeWithReadLock(v -> {
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
        });
    }

    public String getCsrfToken(@NonNull Account account) {
        return executeWithReadLock(
                v -> mAndroidAccountManager.getUserData(account, KEY_CSRF_TOKEN));
    }

    /**
     * Can be called if we get notified about a changed CSRF token.
     * The new token is stored along the user.
     *
     * @param account The account to store the new token along with.
     * @param csrfToken The new token.
     */
    public void updateCsrfToken(@NonNull Account account, @NonNull String csrfToken) {
        executeWithWriteLock(v -> {
            mAndroidAccountManager.setUserData(account, KEY_CSRF_TOKEN, csrfToken);
            return null;
        });
    }

    public void invalidateAuthToken(@NonNull AuthToken authToken) {
        executeWithWriteLock(v -> {
            mAndroidAccountManager.invalidateAuthToken(ACCOUNT_TYPE, authToken.toString());
            return null;
        });
    }

    public void removeAccount(@NonNull String username) {
        Account account = new Account(username, ACCOUNT_TYPE);
        removeAccount(account);
    }
    public void removeAccount(@NonNull Account account) {
        executeWithWriteLock(v -> {
            mAndroidAccountManager.removeAccount(account, null, null);
            return null;
        });
    }

    private <R> R executeWithReadLock(Function < Void, R > f) {
        return executeWithLock(mLock.readLock(), f);
    }
    private <R> R executeWithWriteLock(Function<Void, R> f) {
        return executeWithLock(mLock.writeLock(), f);
    }
    private static <R> R executeWithLock(Lock lock, Function<Void, R> f) {
        try {
            lock.lock();
            return f.apply(null);
        } catch (Exception e) {
            // Ignore.
            return null;
        } finally {
            lock.unlock();
        }
    }

    public class LoginResult {
        private final Response<LoginResponse> mResponse;
        private final AuthData mAuthData;

        LoginResult(Response<LoginResponse> response) {
            this(response, null);
        }

        LoginResult(Response<LoginResponse> response, AuthData authData) {
            mAuthData = authData;
            mResponse = response;
        }

        public boolean isSuccessful() {
            return mResponse.isSuccessful();
        }

        public Response<LoginResponse> response() {
            return mResponse;
        }

        @Nullable
        public AuthData authData() {
            return mAuthData;
        }
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
