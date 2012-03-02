package fi.bitrite.android.ws.activity;

import roboguice.activity.RoboAccountAuthenticatorActivity;
import roboguice.util.Strings;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.inject.Inject;

import fi.bitrite.android.ws.activity.dialog.CredentialsDialog;
import fi.bitrite.android.ws.activity.dialog.DialogHandler;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.AuthenticationService;
import fi.bitrite.android.ws.auth.CredentialsProvider;
import fi.bitrite.android.ws.auth.CredentialsReceiver;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;

/**
 * The activity responsible for getting WarmShowers credentials from the user
 * and storing them on the device using Android's custom account facilities.
 * 
 * TODO: authenticate online as soon as we get credentials here, don't postpone.
 * TODO: Make accounts editable (launch this activity with username in Intent)
 * 
 */
public class AuthenticatorActivity extends RoboAccountAuthenticatorActivity implements CredentialsReceiver {

	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
	public static final int RESULT_AUTHENTICATION_FAILED = RESULT_FIRST_USER + 1;

	private AccountManager accountManager;

	private String username;
	private String password;
	private boolean requestNewAccount;
	
	private DialogHandler dialogHandler;
	
	@Inject HttpAuthenticationService authenticationService;
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		accountManager = AccountManager.get(this);
		dialogHandler = new DialogHandler(this);
		
		Intent intent = getIntent();
		username = intent.getStringExtra(PARAM_USERNAME);
		requestNewAccount = username == null;

		new CredentialsDialog(AuthenticatorActivity.this, AuthenticatorActivity.this, username).show();
	}

	public void applyCredentials(CredentialsProvider credentials) {
		if (Strings.isEmpty(credentials.getUsername()) || Strings.isEmpty(credentials.getPassword())) {
			setResult(RESULT_CANCELED);
			finish();
		} else {
			username = credentials.getUsername();
			password = credentials.getPassword();
			dialogHandler.doOperation(DialogHandler.AUTHENTICATE);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		return dialogHandler.createDialog(id, "Authenticating ...");
	}

	public void doAuthentication() {
		new AuthenticationThread(handler).start();
	}
	
	final Handler handler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			dialogHandler.dismiss();

			Object obj = msg.obj;
			
			if (obj instanceof Exception) {
				setResult(RESULT_AUTHENTICATION_FAILED);
			} else {
				Account account = new Account(username, AuthenticationService.ACCOUNT_TYPE);
				if (requestNewAccount) {
					accountManager.addAccountExplicitly(account, password, null);
				} else {
					Account oldAccount = AuthenticationHelper.getWarmshowersAccount();
					accountManager.removeAccount(oldAccount, null, null);
					accountManager.addAccountExplicitly(account, password, null);
				}
				setResult(RESULT_OK);
			}
			
			finish();
		}
	};

	private class AuthenticationThread extends Thread {
		Handler handler;

		public AuthenticationThread(Handler handler) {
			this.handler = handler;
		}
		
		public void run() {
			Message msg = handler.obtainMessage();
			
			try {
				authenticationService.authenticate(username, password);
				msg.obj = RESULT_OK;
			}
			
			catch (Exception e) {
				Log.e("WSAndroid", e.getMessage(), e);
				msg.obj = e;
			}
			
			handler.sendMessage(msg);
		}
	}
}
