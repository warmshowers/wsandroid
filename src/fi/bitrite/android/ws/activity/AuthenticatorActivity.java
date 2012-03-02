package fi.bitrite.android.ws.activity;

import roboguice.activity.RoboAccountAuthenticatorActivity;
import roboguice.util.Strings;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SyncStateContract.Constants;
import fi.bitrite.android.ws.activity.dialog.CredentialsDialog;
import fi.bitrite.android.ws.auth.AuthenticationService;
import fi.bitrite.android.ws.auth.CredentialsProvider;
import fi.bitrite.android.ws.auth.CredentialsReceiver;

/** 
 * The activity responsible for getting WarmShowers credentials from the user
 * and storing them on the device using Android's custom account facilities.
 * 
 * TODO: authenticate online as soon as we get credentials here, don't postpone.
 * TODO: Make accounts editable (launch this activity with username in Intent)
 * 
 */
public class AuthenticatorActivity extends RoboAccountAuthenticatorActivity implements CredentialsReceiver {

    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
	
	private AccountManager accountManager;
	
	private String username;
	private String authtokenType;
	private String authtoken;
	private boolean requestNewAccount;
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		accountManager = AccountManager.get(this);

		Intent intent = getIntent();
        username = intent.getStringExtra(PARAM_USERNAME);
        authtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);
        requestNewAccount = username == null;
		
		new CredentialsDialog(AuthenticatorActivity.this, AuthenticatorActivity.this, username).show();
	}

	public void applyCredentials(CredentialsProvider credentials) {
		if (Strings.isEmpty(credentials.getUsername()) || Strings.isEmpty(credentials.getPassword())) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		} 
		
		final Account account = new Account(credentials.getUsername(), AuthenticationService.ACCOUNT_TYPE);
		String password = credentials.getPassword();
		
        if (requestNewAccount) {
            accountManager.addAccountExplicitly(account, password, null);
        } else {
            accountManager.setPassword(account, password);
        }

        final Intent intent = new Intent();
        authtoken = password;
        
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);

        if (authtokenType != null && authtokenType.equals(AuthenticationService.ACCOUNT_TYPE)) {
            intent.putExtra(AccountManager.KEY_AUTHTOKEN, authtoken);
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
		finish();
	}
}
