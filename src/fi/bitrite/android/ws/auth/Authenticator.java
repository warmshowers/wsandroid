package fi.bitrite.android.ws.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import fi.bitrite.android.ws.activity.AuthenticatorActivity;

public class Authenticator extends AbstractAccountAuthenticator {

	private final Context context;

	public Authenticator(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
			String[] requiredFeatures, Bundle options) throws NetworkErrorException {
		final Intent intent = new Intent(context, AuthenticatorActivity.class);
		intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType,
			Bundle options) throws NetworkErrorException {
		if (!authTokenType.equals(AuthenticationService.ACCOUNT_TYPE)) {
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authentication token type");
			return result;
		}
		final AccountManager am = AccountManager.get(context);
		final String password = am.getPassword(account);
		if (password != null) {
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, AuthenticationService.ACCOUNT_TYPE);
			result.putString(AccountManager.KEY_AUTHTOKEN, password);
			return result;
		}

		return null;
		// the password was missing or incorrect, return an Intent to an
		// Activity that will prompt the user for the password.
		/*
		 * final Intent intent = new Intent(mContext,
		 * AuthenticatorActivity.class);
		 * intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
		 * intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE,
		 * authTokenType);
		 * intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
		 * response); final Bundle bundle = new Bundle();
		 * bundle.putParcelable(AccountManager.KEY_INTENT, intent); return
		 * bundle;
		 */
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
			throws NetworkErrorException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
			throws NetworkErrorException {
		Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
			Bundle options) throws NetworkErrorException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		throw new UnsupportedOperationException();
	}
}
