package fi.bitrite.android.ws.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.analytics.GoogleAnalytics;

import java.io.IOException;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.AuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpAuthenticator;
import roboguice.activity.RoboAccountAuthenticatorActivity;
import roboguice.inject.InjectView;
import roboguice.util.Strings;

/**
 * The activity responsible for getting WarmShowers credentials from the user,
 * verifying them against the WarmShowers web service and storing and storing
 * them on the device using Android's custom account facilities.
 */
public class AuthenticatorActivity extends RoboAccountAuthenticatorActivity {

    public static final String PARAM_INITIAL_AUTHENTICATION = "initialAuthentication";
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
    public static final int RESULT_AUTHENTICATION_FAILED = RESULT_FIRST_USER + 1;
    public static final int RESULT_NO_NETWORK = 101;

    private AccountManager accountManager;

    @InjectView(R.id.editUsername)
    EditText editUsername;
    @InjectView(R.id.editPassword)
    EditText editPassword;

    private String username;
    private String password;
    private boolean initialAuthentication;

    private DialogHandler dialogHandler;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.credentials);

        accountManager = AccountManager.get(this);
        dialogHandler = new DialogHandler(this);

        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        initialAuthentication = (username == null);
        editUsername.setText(username);
    }

    public void cancel(View view) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(PARAM_INITIAL_AUTHENTICATION, initialAuthentication);
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        cancel(null);
    }

    public void applyCredentials(View view) {
        username = editUsername.getText().toString();
        password = editPassword.getText().toString();
        if (!Strings.isEmpty(username) && !Strings.isEmpty(password)) {
            dialogHandler.showDialog(DialogHandler.AUTHENTICATE);
            Account account = AuthenticationHelper.createNewAccount(username, password);
            doAuthentication(account);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        return dialogHandler.createDialog(id, getResources().getString(R.string.authenticating));
    }

    public void doAuthentication(Account account) {
        new AuthenticationThread(handler, account).start();
    }

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            dialogHandler.dismiss();

            Object obj = msg.obj;
            Intent resultIntent = new Intent();
            resultIntent.putExtra(PARAM_INITIAL_AUTHENTICATION, initialAuthentication);

            if (obj instanceof IOException) {
                setResult(RESULT_NO_NETWORK, resultIntent);
            } else if (obj instanceof Exception) {
                setResult(RESULT_AUTHENTICATION_FAILED, resultIntent);
            } else {
                if (!initialAuthentication) {
                    deleteOldAccount();
                }
                setResult(RESULT_OK, resultIntent);
            }

            finish();
        }


        private void deleteOldAccount() {
            Account oldAccount = AuthenticationHelper.getWarmshowersAccount();
            accountManager.removeAccount(oldAccount, null, null);
        }
    };

    private class AuthenticationThread extends Thread {
        Handler mHandler;
        Account mAccount;
        private static final String TAG = "AuthenticationThread";

        public AuthenticationThread(Handler handler, Account account) {
            mHandler = handler;
            mAccount = account;
        }

        public void run() {
            Message msg = mHandler.obtainMessage();

            try {
                HttpAuthenticator authenticator = new HttpAuthenticator();
                int userId = authenticator.authenticate();

                msg.obj = RESULT_OK;
                msg.arg1 = userId;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                msg.obj = e;
            }

            mHandler.sendMessage(msg);
        }
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

}
