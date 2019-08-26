package fi.bitrite.android.ws.api;

import android.accounts.Account;
import androidx.annotation.WorkerThread;

import java.util.Iterator;

import javax.inject.Inject;

import fi.bitrite.android.ws.api.interceptors.HeaderInterceptor;
import fi.bitrite.android.ws.api.interceptors.ResponseInterceptor;
import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.auth.AuthData;
import fi.bitrite.android.ws.auth.AuthToken;
import fi.bitrite.android.ws.di.account.AccountScope;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import retrofit2.Response;

/**
 * Listens for changes of the {@link AccountManager} to update the header fields in
 * {@link HeaderInterceptor}. Handles authorization errors which occur in
 * {@link ResponseInterceptor}.
 */
@AccountScope
public class AuthenticationController {
    private final AccountManager mAccountManager;
    private final Account mAccount;
    private final WarmshowersAccountWebservice mWebservice;

    private final BehaviorSubject<AuthData> mAuthData = BehaviorSubject.create();

    @Inject
    public AuthenticationController(AccountManager accountManager,
                                    Account account,
                                    HeaderInterceptor headerInterceptor,
                                    ResponseInterceptor responseInterceptor,
                                    WarmshowersAccountWebservice warmshowersWebservice) {
        mAccountManager = accountManager;
        mAccount = account;
        mWebservice = warmshowersWebservice;

        Disposable unused = mAuthData.filter(AuthData::isValid)
                .subscribe(authData -> {
                    headerInterceptor.setSessionCookie(authData.authToken.name,
                            authData.authToken.id);
                    headerInterceptor.setCsrfToken(authData.csrfToken);
                });

        // We handle auth-related API call errors.
        responseInterceptor.setHandler(mResponseInterceptorHandler);

        initAuthData();
    }

    private void initAuthData() {
        Disposable unused = mAccountManager
                // We disallow the user to change their account (the username field is disabled).
                .getAuthToken(mAccount)
                .subscribe(authToken -> {
                    String csrfToken = mAccountManager.getCsrfToken(mAccount);
                    mAuthData.onNext(new AuthData(mAccount, authToken, csrfToken));
                }, e -> {
                    // TODO(saemy): Error handling.
                });
    }

    public BehaviorSubject<AuthData> getAuthData() {
        return mAuthData;
    }

    private final ResponseInterceptor.Handler mResponseInterceptorHandler = new ResponseInterceptor.Handler() {

        /**
         * Error handler for {@link ResponseInterceptor}.
         *
         * Requests a new CSRF token from the API endpoint.
         * This method must be called on a background thread as we wait for the HTTP response to
         * become available.
         *
         * @return True, iff a new token was optained.
         */
        @WorkerThread
        @Override
        public boolean handleCsrfValidationError() {
            // Waits for the response to become available.
            try {
                Response<String> response =
                        mWebservice.renewCsrfToken().blockingFirst();

                if (response.isSuccessful()) {
                    Account account = mAuthData.getValue().account;
                    AuthToken authToken = mAuthData.getValue().authToken;
                    String csrfToken = response.body();

                    // Updates the CSRF token in the account database.
                    mAccountManager.updateCsrfToken(account, csrfToken);

                    // Updates the cached auth data. This triggers all necessary updates.
                    mAuthData.onNext(new AuthData(account, authToken, csrfToken));

                    return true;
                } else {
                    // TODO(saemy): Http error handling (log errorBody()?)
                    return false;
                }
            } catch (Throwable e) {
                // TODO(saemy): IOException handling
                return false;
            }
        }

        /**
         * Error handler for {@link ResponseInterceptor}.
         *
         * Invalidates the current authToken and re-authenticates the user to obtain a new auth
         * token from the API endpoint.
         *
         * @return True, iff a new auth token was obtained.
         */
        @Override
        public boolean handleAuthTokenExpiration(AuthToken oldAuthToken) {
            AuthData authData = mAuthData.getValue();
            if (authData != null && authData.authToken.equals(oldAuthToken)) {
                synchronized (this) {
                    authData = mAuthData.getValue();
                    if (authData != null && authData.authToken.equals(oldAuthToken)) {
                        // Invalidates the current auth token s.t. it gets updated.
                        mAccountManager.invalidateAuthToken(authData.authToken);
                        mAuthData.onNext(new AuthData());

                        // Triggers another requests for the auth token.
                        initAuthData();
                    }
                }
            }

            return waitForAuthToken();
        }

        /**
         * Handler for {@link ResponseInterceptor}.
         *
         * Waits until the auth token becomes available.
         *
         * @return True, iff no error occured.
         */
        @Override
        public boolean waitForAuthToken() {
            if (mAuthData.hasValue() && mAuthData.getValue().isValid()) {
                return true;
            }

            // Maximal number of authData elements we are waiting for until we abort.
            int maxIterations = 2;

            Iterator<AuthData> it = mAuthData.blockingNext().iterator(); // FIXME(saemy): Remove blocking.
            AuthData authData;
            do {
                authData = it.next();
            } while (!authData.isValid() && --maxIterations > 0);

            return authData.isValid();
        }
    };
}
