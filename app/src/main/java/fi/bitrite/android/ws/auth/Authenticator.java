package fi.bitrite.android.ws.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.WarmshowersWebservice;
import fi.bitrite.android.ws.api.response.LoginResponse;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.AuthenticatorActivity;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class Authenticator extends AbstractAccountAuthenticator {

    private final Context mContext;
    private final AccountManager mAccountManager;
    private final WarmshowersWebservice mGeneralWebservice;
    private final UserRepository.AppUserRepository mAppUserRepository;

    @Inject
    public Authenticator(Context context,
                         @NonNull AccountManager accountManager,
                         WarmshowersWebservice generalWebservice,
                         UserRepository.AppUserRepository appUserRepository) {
        super(context);

        mContext = context;
        mAccountManager = accountManager;
        mGeneralWebservice = generalWebservice;
        mAppUserRepository = appUserRepository;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures,
                             Bundle options) {
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class)
                // Used by {@link AuthenticatorActivity}.
                .putExtra(android.accounts.AccountManager.KEY_ACCOUNT_TYPE, accountType)

                // Used by {@link android.accounts.AccountAuthenticatorAcitvity}.
                .putExtra(android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                        response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(android.accounts.AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) {
        // Checks if the auth token is already available.
        AuthToken authToken = mAccountManager.peekAuthToken(account);
        if (authToken == null) {
            final String password = mAccountManager.getPassword(account);
            if (!TextUtils.isEmpty(password)) {
                // Try a re-login with the stored password.
                AuthResult authResult = login(account, password, true).blockingFirst();
                authToken = authResult.authToken; // Might be null if not successful.
            }
        }
        if (authToken != null) {
            // We got the token.
            // These values are required by the {@link android.accounts.AbstractAccountAuthenticator}.
            final Bundle result = new Bundle();
            result.putString(android.accounts.AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(android.accounts.AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(android.accounts.AccountManager.KEY_AUTHTOKEN, authToken.toString());
            return result;
        }

        // We were not able to get the token. This means it was invalidated at some other
        // point -> re-login.
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class)
                // Used by {@link AuthenticatorActivity}.
                .putExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME, account.name)
                .putExtra(android.accounts.AccountManager.KEY_ACCOUNT_TYPE, account.type)

                // Used by {@link android.accounts.AccountAuthenticatorActivity}.
                .putExtra(android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                        response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(android.accounts.AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                                     Bundle options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle hasFeatures(
            AccountAuthenticatorResponse response, Account account, String[] features) {
        Bundle result = new Bundle();
        result.putBoolean(android.accounts.AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle updateCredentials(
            AccountAuthenticatorResponse response, Account account, String authTokenType,
            Bundle options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    /**
     * Tries to log the given user in. On success, the account information is put into the Android
     * account service. Any account with the same username is updated or, if none found, a new one
     * is created.
     *
     * @return The login result
     */
    public Observable<AuthResult> login(Account account, String password, boolean storePassword) {
        return mGeneralWebservice.login(account.name, password)
                .subscribeOn(Schedulers.io())
                .flatMap(response -> {
                    if (!response.isSuccessful()) {
                        return Observable.just(response);
                    }

                    // Stores the account in the repository. With this it is already available
                    // without the need of any further network accesses.
                    return mAppUserRepository.save(response.body().user.toUser())
                            .toSingle(() -> response)
                            .toObservable();
                })
                .map(response -> {
                    if (!response.isSuccessful()) {
                        String errorMsgRaw = response.errorBody().string();
                        Log.w(WSAndroidApplication.TAG, String.format(
                                "Auth failure. Code=%d, Message=%s",
                                response.code(),
                                errorMsgRaw));

                        // Strip enclosing `[""]` from the error body.
                        String errorMsg = errorMsgRaw;
                        if (errorMsg.startsWith("[\"") && errorMsg.endsWith("\"]")) {
                            errorMsg = errorMsg.substring(2, errorMsg.length()-2);
                        }

                        AuthResult.ErrorCause errorCause = AuthResult.ErrorCause.Unknown;
                        if (response.code() == 401) {
                            if (errorMsg.equals("Wrong username or password.")) {
                                errorCause = AuthResult.ErrorCause.WrongUsernameOrPassword;
                            } else if (errorMsg.equals("HTTP Authorization failure credentials not present")
                                       || errorMsg.equals("HTTP Authorization developer account does not have API key")
                                       || errorMsg.equals("HTTP Authorization developer account API key does not match HTTP_AUTHORIZATION API key")
                                       || errorMsg.equals("HTTP Authorization failure, developer UID user load not found")) {
                                errorCause = AuthResult.ErrorCause.WrongAPIKey;
                            }
                        } else if (response.code() == 406) {
                            // 406: ["Already logged in as xxx."]
                            // 406: ["Account is temporarily blocked."]
                            // 406: ["This IP address is temporarily blocked."]
                            if (errorMsg.startsWith("Already logged in as ")) {
                                // This error can occur if an additional account is added, which in fact
                                // is already logged in. We just mark the login as successful and
                                // continue.
                                return AuthResult.success(mAccountManager.peekAuthToken(account));
                            } else if (errorMsg.equals("Account is temporarily blocked.")) {
                                errorCause = AuthResult.ErrorCause.AccountTemporarilyBlocked;
                            } else if (errorMsg.equals("This IP address is temporarily blocked.")) {
                                errorCause = AuthResult.ErrorCause.IpTemporarilyBlocked;
                            }
                        }

                        return AuthResult.error(response.code(), errorMsg, errorCause);
                    }

                    // Creates a new or updates an existing account.
                    LoginResponse loginResponse = response.body();
                    int userId = loginResponse.user.id;
                    String csrfToken = loginResponse.csrfToken;
                    AuthToken authToken =
                            new AuthToken(loginResponse.sessionName, loginResponse.sessionId);
                    AuthData authData = new AuthData(account, authToken, csrfToken);
                    mAccountManager.updateOrCreateAccount(
                            authData, userId, storePassword ? password : null);

                    // Fires the callback.
                    return AuthResult.success(authToken);
                });
    }

    public static class AuthResult {
        public final int statusCode;
        public final AuthToken authToken;
        public final String errorMessage;

        public enum ErrorCause {
            NoError,
            WrongUsernameOrPassword,
            WrongAPIKey,
            AccountTemporarilyBlocked,
            IpTemporarilyBlocked,
            Unknown;
        }
        public final ErrorCause errorCause;

        public static AuthResult success(AuthToken authToken) {
            return new AuthResult(200, authToken, null, ErrorCause.NoError);
        }

        public static AuthResult error(int statusCode, String errorMessage, ErrorCause errorCause) {
            return new AuthResult(statusCode, null, errorMessage, errorCause);
        }

        private AuthResult(int statusCode, AuthToken authToken, String errorMessage, ErrorCause errorCause) {
            this.statusCode = statusCode;
            this.authToken = authToken;
            this.errorMessage = errorMessage;
            this.errorCause = errorCause;
        }

        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
