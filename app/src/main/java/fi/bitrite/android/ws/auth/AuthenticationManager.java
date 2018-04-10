package fi.bitrite.android.ws.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.api.WarmshowersService;
import fi.bitrite.android.ws.api.response.LoginResponse;
import fi.bitrite.android.ws.ui.AuthenticatorActivity;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import retrofit2.Response;

@Singleton
public class AuthenticationManager {
    private static final String ACCOUNT_TYPE = "org.warmshowers";
    private static final String AUTH_TOKEN_TYPE = "FULL_ACCESS";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_CSRF_TOKEN = "csrf_token";

    private final AccountManager mAccountManager;
    private final WarmshowersService mWarmshowersService;

    private final ReentrantReadWriteLock mAuthenticationManagerLock = new ReentrantReadWriteLock();

    private final BehaviorSubject<Account[]> mAccounts;

    @Inject
    public AuthenticationManager(AccountManager accountManager,
                                 WarmshowersService warmshowersService) {
        mAccountManager = accountManager;
        mWarmshowersService = warmshowersService;

        mAccounts = BehaviorSubject.createDefault(mAccountManager.getAccountsByType(ACCOUNT_TYPE));
        mAccountManager.addOnAccountsUpdatedListener(accounts -> {
            Account[] newAccounts = mAccountManager.getAccountsByType(ACCOUNT_TYPE);
            if (!Arrays.equals(mAccounts.getValue(), newAccounts)) {
                mAccounts.onNext(newAccounts);
            }
        }, null, false);
    }

    /**
     * Creates a new account, the {@link AuthenticatorActivity} is
     * shown to the user.
     *
     * @param activity The activity that is used to launch the {@link AuthenticatorActivity} if needed.
     * @return The new account as soon as it becomes available.
     */
    public Observable<Account> createNewAccount(Activity activity) {
        return Single.<Account>create(emitter -> {
            Bundle options = new Bundle();

            AccountManagerCallback<Bundle> accountManagerCallback = accountManagerFuture -> {
                try {
                    Bundle result = accountManagerFuture.getResult();

                    String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                    String type = result.getString(AccountManager.KEY_ACCOUNT_TYPE);

                    // TODO(saemy): Mark this account as the active one.

                    emitter.onSuccess(new Account(name, type));
                } catch (Exception e) {
                    emitter.onError(e);
                }
            };

            // Asks the user to create the new account. This is done by showing the login page.
            mAccountManager.addAccount(
                    ACCOUNT_TYPE, AUTH_TOKEN_TYPE, null, options, activity, accountManagerCallback,
                    null);
        }).toObservable();
    }

    /**
     * Tries to log the given user in. On success, the account information is put into the Android
     * account service. Any account with the same username is updated or, if none found, a new one
     * is created.
     *
     * @param username
     * @param password
     * @return The login result
     */
    public Observable<LoginResult> login(String username, String password) {
        return mWarmshowersService.login(username, password)
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
                        if (!isExistingAccount(account)) {
                            // This explicitly does not save any password as it is stored in
                            // plaintext on the device. On rooted devices this is an issue! Instead,
                            // the auth token is stored along the account to avoid re-logins.
                            mAccountManager.addAccountExplicitly(account, null, null);

                            // Sets the user id.
                            int userId = loginResponse.user.id;
                            mAccountManager.setUserData(
                                    account, KEY_USER_ID, Integer.toString(userId));
                        }

                        // Updates the CSRF token.
                        String csrfToken = loginResponse.csrfToken;
                        mAccountManager.setUserData(account, KEY_CSRF_TOKEN, csrfToken);

                        // Fetches the auth token from the login response.
                        AuthToken authToken =
                                new AuthToken(loginResponse.sessionName, loginResponse.sessionId);
                        mAccountManager.setAuthToken(
                                account, AUTH_TOKEN_TYPE, authToken.toString());

                        // Fires the callback.
                        AuthData authData = new AuthData(account, authToken, csrfToken);
                        return new LoginResult(response, authData);
                    });
                });
    }

    private boolean isExistingAccount(@NonNull Account account) {
        // The AccountManager does not need to be synchronized (see {@link #login}) as the set of
        // accounts is not affected of any race.
        Account[] accounts = mAccountManager.getAccountsByType(ACCOUNT_TYPE);
        for (Account existingAccount : accounts) {
            if (existingAccount.equals(account)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the accounts that are already stored in the Android account system.
     *
     * @return The accounts
     */
    public BehaviorSubject<Account[]> getExistingAccounts() {
        return mAccounts;
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
            String authTokenStr = mAccountManager.peekAuthToken(account, AUTH_TOKEN_TYPE);
            return authTokenStr == null
                    ? null
                    : AuthToken.fromString(authTokenStr);
        });
    }

    /**
     * Gets the authToken of the given account from the Android account service. If the token is not
     * available, the {@link AuthenticatorActivity} is shown to the
     * user s.t. they can re-login.
     *
     * @param activity The activity that is used to launch the {@link AuthenticatorActivity} if needed.
     * @param account The account to get the authToken for.
     * @return The authToken as soon as it becomes available.
     */
    public Single<AuthToken> getAuthToken(@NonNull Account account, Activity activity) {
        return Single.create(emitter -> {
            AccountManagerCallback<Bundle> accountManagerCallback = tokenFuture -> {
                try {
                    Bundle result = tokenFuture.getResult();

                    String authTokenStr = result.getString(AccountManager.KEY_AUTHTOKEN);
                    AuthToken authToken = AuthToken.fromString(authTokenStr);

                    emitter.onSuccess(authToken);
                } catch (Exception e) {
                    emitter.onError(e);
                }
            };

            executeWithReadLock(v -> {
                mAccountManager.getAuthToken(
                        account, AUTH_TOKEN_TYPE, null, activity, accountManagerCallback, null);
                return null;
            });
        });
    }

    public int getUserId(@NonNull Account account) {
        return executeWithReadLock(v -> {
            String userIdStr = mAccountManager.getUserData(account, KEY_USER_ID);
            return userIdStr != null
                    ? Integer.parseInt(userIdStr)
                    : fi.bitrite.android.ws.auth.AccountManager.UNKNOWN_USER_ID;
        });
    }

    public String getCsrfToken(@NonNull Account account) {
        return executeWithReadLock(v -> mAccountManager.getUserData(account, KEY_CSRF_TOKEN));
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
            mAccountManager.setUserData(account, KEY_CSRF_TOKEN, csrfToken);
            return null;
        });
    }

    public void invalidateAuthToken(@NonNull AuthToken authToken) {
        executeWithWriteLock(v -> {
            mAccountManager.invalidateAuthToken(ACCOUNT_TYPE, authToken.toString());
            return null;
        });
    }

    public void removeAccount(@NonNull String username) {
        Account account = new Account(username, ACCOUNT_TYPE);
        removeAccount(account);
    }

    public void removeAccount(@NonNull Account account) {
        executeWithWriteLock(v -> {
            mAccountManager.removeAccount(account, null, null);
            return null;
        });
    }

    private <R> R executeWithReadLock(Function<Void, R> f) {
        return executeWithLock(mAuthenticationManagerLock.readLock(), f);
    }

    private <R> R executeWithWriteLock(Function<Void, R> f) {
        return executeWithLock(mAuthenticationManagerLock.writeLock(), f);
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
}
