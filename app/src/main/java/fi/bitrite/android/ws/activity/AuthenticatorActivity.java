package fi.bitrite.android.ws.activity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationManager;
import fi.bitrite.android.ws.di.Injectable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * The activity responsible for getting Warmshowers credentials from the user,
 * verifying them against the Warmshowers web service and storing
 * them on the device using Android's custom account facilities.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity implements Injectable {

    @Inject AuthenticationManager mAuthenticationManager;

    private TextView edtUsername;
    private TextView edtPassword;
    private Button btnLogin;

    private DialogHandler mDialogHandler;

    private String mAccountType;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_authentication);

        edtUsername = findViewById(R.id.editUsername);
        edtPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);

        mDialogHandler = new DialogHandler(this);

        // If we do a re-login (to obtain a new auth token), the username is provided by the caller.
        String providedUsername = getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        if (!TextUtils.isEmpty(providedUsername)) {
            edtUsername.setText(providedUsername);
            edtUsername.setInputType(InputType.TYPE_NULL); // Disallow username modification.

            // The username is set -> we set the focus on the password field.
            edtPassword.requestFocus();
        }

        btnLogin.setOnClickListener(this::login);

        // Shows the keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    public void login(View view) {
        String username = edtUsername.getText().toString();
        String password = edtPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            return;
        }

        mDialogHandler.showDialog(DialogHandler.AUTHENTICATE);

        mAuthenticationManager.login(username, password)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    Bundle response = new Bundle();
                    response.putString(AccountManager.KEY_ACCOUNT_NAME, username);

                    String accountType = getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
                    response.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);

                    boolean isAlreadyLoggedIn = 406 == result.response().code();
                    if (result.isSuccessful() || isAlreadyLoggedIn) {
                        String authToken;
                        if (isAlreadyLoggedIn) {
                            // 406 Not Acceptable : Already logged in as [xxx]. ()
                            // This error can occur if an additional account is added, which in fact
                            // is already logged in. We just mark the login as successful and
                            // continue.

                            // We just used that accounts authToken during the login request. So we
                            // can safely get it from the AuthenticationManager in a blocking manner.
                            Account account = new Account(username, accountType);
                            authToken = mAuthenticationManager.getAuthToken(account)
                                    .blockingGet().toString();
                        } else {
                            // 200 OK - The login was successful.
                            authToken = result.authData().authToken.toString();
                        }

                        // Required if this activity was shown in getAuthToken but the authToken was
                        // previously invalidated.
                        response.putString(AccountManager.KEY_AUTHTOKEN, authToken);

                        setAccountAuthenticatorResult(response);

                        finish();
                    } else {
                        mDialogHandler.dismiss();

                        Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG)
                                .show();
                    }
                }, error -> {
                    mDialogHandler.dismiss();

                    Toast.makeText(this, R.string.http_server_access_failure, Toast.LENGTH_SHORT)
                            .show();
                });
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        // TODO(saemy): Deprecated.
        return mDialogHandler.createDialog(id, getResources().getString(R.string.authenticating));
    }
}
