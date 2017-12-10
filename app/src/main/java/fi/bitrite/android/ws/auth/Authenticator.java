package fi.bitrite.android.ws.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import javax.inject.Inject;

import fi.bitrite.android.ws.activity.AuthenticatorActivity;

public class Authenticator extends AbstractAccountAuthenticator {

    private final AccountManager accountManager;

    private final Context context;

    @Inject
    public Authenticator(Context context, @NonNull AccountManager accountManager) {
        super(context);

        this.accountManager = accountManager;
        this.context = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures,
                             Bundle options) throws NetworkErrorException{
        final Intent intent = new Intent(context, AuthenticatorActivity.class)
                // Used by {@link AuthenticatorActivity}.
                .putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType)

                // Used by {@link android.accounts.AccountAuthenticatorAcitvity}.
                .putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) throws NetworkErrorException {
        // Checks if the auth token is already available.
        String authToken = accountManager.peekAuthToken(account, authTokenType);
        if (!TextUtils.isEmpty(authToken)) {
            // We got the token.
            // These values are required by the {@link android.accounts.AbstractAccountAuthenticator}.
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // We were not able to get the token. This means it was invalidated at some other
        // point -> re-login.
        final Intent intent = new Intent(context, AuthenticatorActivity.class)
                // Used by {@link AuthenticatorActivity}.
                .putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name)
                .putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type)

                // Used by {@link android.accounts.AccountAuthenticatorActivity}.
                .putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                                     Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle hasFeatures(
            AccountAuthenticatorResponse response, Account account, String[] features)
            throws NetworkErrorException {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle updateCredentials(
            AccountAuthenticatorResponse response, Account account, String authTokenType,
            Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }
}
