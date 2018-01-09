package fi.bitrite.android.ws.api_new;

import android.accounts.Account;
import android.app.Activity;
import android.support.annotation.WorkerThread;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.api_new.interceptors.HeaderInterceptor;
import fi.bitrite.android.ws.api_new.interceptors.ResponseInterceptor;
import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.auth.AuthData;
import fi.bitrite.android.ws.auth.AuthToken;
import fi.bitrite.android.ws.auth.AuthenticationManager;
import fi.bitrite.android.ws.ui.MainActivity;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import retrofit2.Response;

/**
 * Listens for changes of the {@link AuthenticationManager} to update the header fields in
 * {@link HeaderInterceptor}. Handles authorization errors which occur in
 * {@link ResponseInterceptor}.
 */
// TODO(saemy): This must become testable!
@Singleton
public class AuthenticationController {

    private final AccountManager mAccountManager;
    private final AuthenticationManager mAuthenticationManager;
    private final WarmshowersService mWarmshowersService;

    private final BehaviorSubject<AuthData> mAuthData = BehaviorSubject.create();

    private Activity mMainActivity = null;

    @Inject
    public AuthenticationController(
            AccountManager accountManager, AuthenticationManager authenticationManager,
            HeaderInterceptor headerInterceptor, ResponseInterceptor responseInterceptor,
            WarmshowersService warmshowersService) {
        mAccountManager = accountManager;
        mAuthenticationManager = authenticationManager;
        mWarmshowersService = warmshowersService;

        mAuthData
                .filter(AuthData::isValid)
                .subscribe(authData -> {
                    headerInterceptor.setSessionCookie(authData.authToken.name, authData.authToken.id);
                    headerInterceptor.setCsrfToken(authData.csrfToken);
                });

        // We handle auth-related API call errors.
        responseInterceptor.setHandler(mResponseInterceptorHandler);
    }

    /**
     * Initializes this controller async. If no account is created yet, the login activity is shown.
     * This function must not be called before an activity is started.
     */
    public void init(MainActivity mainActivity) {
        if (isInitialized()) {
            throw new RuntimeException("AuthenticationController::init must not be called twice.");
        }
        mMainActivity = mainActivity;

        // Gets the existing accounts.
        mAccountManager.getCurrentAccount()
                .map(account -> {
                    if (account.isNonNull()) {
                        // There already is an account.
                        return Observable.just(account.data);
                    } else {
                        // Creates a new account by showing the login activity.
                        return mAuthenticationManager.createNewAccount(mMainActivity);
                    }
                })
                .flatMap(account -> initAuthData(account).toObservable())
                .subscribe(o -> {}, e -> {
                    // TODO(saemy): Exception handling...
                    mAuthData.onNext(new AuthData());
                });
    }

    public boolean isInitialized() {
        return mMainActivity != null;
    }

    private Completable initAuthData(Observable<Account> accountSingle) {
        final AtomicReference<Account> account = new AtomicReference<>(); // To have a final variable...
        return Completable.create(emitter -> {
            accountSingle
                    .flatMap(acc -> {
                        account.set(acc);

                        // We disallow the user to change their account (the username field is not
                        // editable).
                        return mAuthenticationManager.getAuthToken(acc, mMainActivity).toObservable();
                    })
                    .subscribe(authToken -> {
                        String csrfToken = mAuthenticationManager.getCsrfToken(account.get());

                        mAuthData.onNext(new AuthData(account.get(), authToken, csrfToken));

                        emitter.onComplete();
                    }, emitter::onError);
        });
    }

    public BehaviorSubject<AuthData> getAuthData() {
        return mAuthData;
    }

    public ResponseInterceptor.Handler getResponseInterceptorHandler() {
        // TODO(saemy): Remove this accessor when RestClient is no longer in use.
        return mResponseInterceptorHandler;
    }

    private final ResponseInterceptor.Handler mResponseInterceptorHandler =
            new ResponseInterceptor.Handler() {

        /**
         * Error handler from {@link ResponseInterceptor}.
         *
         * Requests a new CSRF token from the API endpoint.
         * This method must be called on a background thread as we wait for the HTTP response to
         * become available.
         *
         * @return True, iff a new token was optained.
         */
        @WorkerThread
        @Override
        public boolean handleCsrfValidationError() throws IOException {
            // Waits for the response to become available.
            try {
                Response<String> response = mWarmshowersService.renewCsrfToken().blockingFirst();

                if (response.isSuccessful()) {
                    Account account = mAuthData.getValue().account;
                    AuthToken authToken = mAuthData.getValue().authToken;
                    String csrfToken = response.body();

                    // Updates the CSRF token in the account database.
                    mAuthenticationManager.updateCsrfToken(account, csrfToken);

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
         * Error handler from {@link ResponseInterceptor}.
         *
         * Invalidates the current authToken and re-authenticates the user to obtain a new auth
         * token from the API endpoint.
         *
         * @return True, iff a new auth token was obtained.
         */
        @Override
        public boolean handleAuthTokenExpiration() {
            // Invalidates the current auth token s.t. it gets updated.
            mAuthenticationManager.invalidateAuthToken(mAuthData.getValue().authToken);

            // Resets the account container. This reloads the account which in turn requires the
            // auth token to be updated.
            // Waits for the auth token to show up.
            try {
                initAuthData(Observable.just(mAuthData.getValue().account))
                        .blockingAwait();

                return true;
            } catch (Throwable e) {
                return false;
            }
        }

        /**
         * Handler from {@link ResponseInterceptor}.
         *
         * Waits until the auth token becomes available.
         *
         * @return True, iff no error occured.
         */
        @Override
        public boolean waitForAuthToken() {
            while (mAuthData.getValue() == null) {
                mAuthData.blockingNext();
            }
            // TODO(saemy): Handle error case (that currently leads to a endless loop).

            return true;
        }
    };
}
