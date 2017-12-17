package fi.bitrite.android.ws.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthToken;
import fi.bitrite.android.ws.auth.AuthenticationManager;
import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.ui.view.AccountAuthenticatorFragmentActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * The activity responsible for getting Warmshowers credentials from the user,
 * verifying them against the Warmshowers web service and storing
 * them on the device using Android's custom account facilities.
 */
public class AuthenticatorActivity extends AccountAuthenticatorFragmentActivity implements Injectable {

    @Inject AuthenticationManager mAuthenticationManager;

    @BindView(R.id.auth_txt_username) TextView mTxtUsername;
    @BindView(R.id.auth_txt_password) TextView mTxtPassword;

    private ProgressDialog.Disposable mProgressDisposable;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_authentication);
        ButterKnife.bind(this);

        // If we do a re-login (to obtain a new auth token), the username is provided by the caller.
        String providedUsername = getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        if (!TextUtils.isEmpty(providedUsername)) {
            mTxtUsername.setText(providedUsername);
            mTxtUsername.setInputType(InputType.TYPE_NULL); // Disallow username modification.

            // The username is set -> we set the focus on the password field.
            mTxtPassword.requestFocus();
        }

        // Shows the keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @OnClick(R.id.all_btn_submit)
    public void login() {
        String username = mTxtUsername.getText().toString();
        String password = mTxtPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            return;
        }

        mProgressDisposable = ProgressDialog.create(R.string.authenticating)
                .show(this);

        // FIXME(saemy): This call fails if the screen rotation is changed while it is on the fly.
        mAuthenticationManager.login(username, password)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    Bundle response = new Bundle();
                    response.putString(AccountManager.KEY_ACCOUNT_NAME, username);

                    String accountType = getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
                    response.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);

                    boolean isAlreadyLoggedIn = 406 == result.response().code();
                    if (result.isSuccessful() || isAlreadyLoggedIn) {
                        AuthToken authToken;
                        if (isAlreadyLoggedIn) {
                            // 406 Not Acceptable : Already logged in as [xxx]. ()
                            // This error can occur if an additional account is added, which in fact
                            // is already logged in. We just mark the login as successful and
                            // continue.
                            Account account = new Account(username, accountType);
                            authToken = mAuthenticationManager.peekAuthToken(account);
                        } else {
                            // 200 OK - The login was successful.
                            authToken = result.authData().authToken;
                        }

                        // Required if this activity was shown in getAuthToken but the authToken was
                        // previously invalidated.
                        String authTokenStr = authToken == null ? null : authToken.toString();
                        response.putString(AccountManager.KEY_AUTHTOKEN, authTokenStr);

                        setAccountAuthenticatorResult(response);

                        finish();
                    } else {
                        mProgressDisposable.dispose();

                        Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG)
                                .show();
                    }
                }, error -> {
                    mProgressDisposable.dispose();

                    Toast.makeText(this, R.string.http_server_access_failure, Toast.LENGTH_SHORT)
                            .show();
                });
    }
}
