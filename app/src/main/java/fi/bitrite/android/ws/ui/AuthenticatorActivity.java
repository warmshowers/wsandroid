package fi.bitrite.android.ws.ui;

import android.accounts.Account;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.Authenticator;
import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.ui.view.AccountAuthenticatorFragmentActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

/**
 * The activity responsible for getting Warmshowers credentials from the user,
 * verifying them against the Warmshowers web service and storing
 * them on the device using Android's custom account facilities.
 */
public class AuthenticatorActivity extends AccountAuthenticatorFragmentActivity
        implements Injectable {

    @Inject Authenticator mAuthenticator;

    @BindView(R.id.auth_txt_username) EditText mTxtUsername;
    @BindView(R.id.auth_txt_password) EditText mTxtPassword;
    @BindView(R.id.auth_ckb_remember_password) CheckBox mCkbRememberPassword;
    @BindView(R.id.auth_btn_login) Button mBtnLogin;

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            setLoginButtonEnabled(areAllInputsNotEmpty());
        }
    };

    private Disposable mProgressDisposable;

    private Disposable mResumePauseDisposable;
    private BehaviorSubject<LoginResult> mLoginResult = BehaviorSubject.create();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_authentication);
        ButterKnife.bind(this);

        // Deactivates login button to prevent requests from being made with incomplete login data
        setLoginButtonEnabled(areAllInputsNotEmpty());
        mTxtUsername.addTextChangedListener(mTextWatcher);
        mTxtPassword.addTextChangedListener(mTextWatcher);

        // If we do a re-login (to obtain a new auth token), the username is provided by the caller.
        String providedUsername =
                getIntent().getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME);
        if (!TextUtils.isEmpty(providedUsername)) {
            mTxtUsername.setText(providedUsername);
            disableEditText(mTxtUsername);

            // The username is set -> we set the focus on the password field.
            mTxtPassword.requestFocus();
        }

        // Shows the keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We only listen to login results as long as the app is in the foreground. If it was in the
        // background when the result appeared we catch up on taking care of it as the very next
        // thing.
        mResumePauseDisposable = mLoginResult
                .filter(result -> !result.isHandled)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    result.isHandled = true;

                    if (result.throwable != null) {
                        mProgressDisposable.dispose();
                        Toast.makeText(this, R.string.http_server_access_failure, Toast.LENGTH_LONG)
                                .show();
                    } else if (result.authResult != null && result.authResult.isSuccessful()) {
                        Bundle bundle = new Bundle();
                        bundle.putString(android.accounts.AccountManager.KEY_ACCOUNT_NAME,
                                result.account.name);
                        bundle.putString(android.accounts.AccountManager.KEY_ACCOUNT_TYPE,
                                result.account.type);

                        // Required if this activity was shown in getAuthToken but the authToken was
                        // previously invalidated.
                        String authTokenStr = result.authResult.authToken == null
                                ? null
                                : result.authResult.authToken.toString();
                        bundle.putString(android.accounts.AccountManager.KEY_AUTHTOKEN,
                                authTokenStr);

                        // The following we add for usage in MainActivity::onActivityResult().
                        Intent intent = new Intent();
                        intent.putExtras(bundle);
                        setResult(0, intent);
                        setAccountAuthenticatorResult(bundle);
                        finish();
                    } else {
                        mProgressDisposable.dispose();

                        Authenticator.AuthResult.ErrorCause errorCause = result.authResult != null
                                ? result.authResult.errorCause
                                : Authenticator.AuthResult.ErrorCause.Unknown;

                        @StringRes final int messageId =
                                errorCause == Authenticator.AuthResult.ErrorCause.WrongUsernameOrPassword
                                ? R.string.authentication_failed
                                // Wrong password provided by the user.
                                : R.string.invalid_api_key;
                                // Issues with the API key or the API itself.
                                // Used to be in case of `statusCode \in {401,404}`.
                        Toast.makeText(this, messageId, Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    @Override
    protected void onPause() {
        mResumePauseDisposable.dispose();
        super.onPause();
    }

    /**
     * Changes the enabled state of the login button
     *
     * @param enabled If true, login button is set to activated, if false it's deactivated
     */
    @VisibleForTesting
    void setLoginButtonEnabled(boolean enabled) {
        mBtnLogin.setEnabled(enabled);
    }

    /**
     * @return True if both username and password input are empty, false otherwise.
     */
    @VisibleForTesting
    boolean areAllInputsNotEmpty() {
        return !TextUtils.isEmpty(mTxtUsername.getText())
               && !TextUtils.isEmpty(mTxtPassword.getText());
    }

    /**
     * Completely disables specified edit text, rendering it like a TextView.
     *
     * @param editText EditText which should be disabled
     */
    private void disableEditText(EditText editText) {
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setCursorVisible(false);
        editText.setEnabled(false);
        editText.setFocusable(false);
        editText.setKeyListener(null);
    }


    @OnClick(R.id.auth_btn_login)
    public void login() {
        String username = mTxtUsername.getText().toString();
        String password = mTxtPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            return;
        }

        mProgressDisposable = ProgressDialog.create(R.string.authenticating).show(this);

        // The following callback can be happening when the app is pushed to the background. We
        // update the {@link mLoginResult} observable, s.t. we only react on the change when the app
        // is in the foreground.
        String accountType = getIntent().getStringExtra(
                android.accounts.AccountManager.KEY_ACCOUNT_TYPE);
        Account account = new Account(username, accountType);
        Disposable unused = mAuthenticator.login(account, password, mCkbRememberPassword.isChecked())
                .subscribe(result -> mLoginResult.onNext(new LoginResult(account, result)),
                        error -> mLoginResult.onNext(new LoginResult(account, error)));
    }

    /**
     * Starts the user's browser and opens the password reset page
     * for Warmshowers if forgot password button was clicked
     */
    @OnClick(R.id.auth_btn_forgot_password)
    public void forgotPassword() {
        final String passwordResetPageUrl = getString(R.string.url_target_forgot_password);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(passwordResetPageUrl)));
    }

    @Override
    public void onBackPressed() {
        // Disallow it.
    }

    private class LoginResult {
        @NonNull final Account account;
        final Authenticator.AuthResult authResult;
        final Throwable throwable;
        boolean isHandled = false;

        LoginResult(@NonNull Account account, @NonNull Authenticator.AuthResult authResult) {
            this.account = account;
            this.authResult = authResult;
            this.throwable = null;
        }
        LoginResult(@NonNull Account account, @NonNull Throwable throwable) {
            this.account = account;
            this.authResult = null;
            this.throwable = throwable;
        }
    }
}
