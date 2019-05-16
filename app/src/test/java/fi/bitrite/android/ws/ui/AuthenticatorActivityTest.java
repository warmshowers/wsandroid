package fi.bitrite.android.ws.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import fi.bitrite.android.ws.auth.AuthToken;
import fi.bitrite.android.ws.auth.Authenticator;
import io.reactivex.Observable;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AuthenticatorActivityTest {

    private final static String MOCK_ACCOUNT_TYPE = "mockAccountType";
    private Intent intent;
    private AuthenticatorActivity authenticator;

    @Before
    public void setUp() {
        intent = new Intent(RuntimeEnvironment.application, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, MOCK_ACCOUNT_TYPE);
        authenticator = Robolectric.buildActivity(AuthenticatorActivity.class, intent)
                .setup()
                .get();
    }

    @Test
    public void areInputsNotEmpty_empty_returnsEmpty() {
        authenticator.mTxtUsername.setText("");
        authenticator.mTxtPassword.setText("");

        assertThat(authenticator.areAllInputsNotEmpty()).isFalse();
    }

    @Test
    public void areInputsNotEmpty_notEmpty_returnsNotEmpty() {
        authenticator.mTxtUsername.setText("username");
        authenticator.mTxtPassword.setText("password");

        assertThat(authenticator.areAllInputsNotEmpty()).isTrue();
    }

    @Test
    public void areInputsNotEmpty_null_returnsEmpty() {
        authenticator.mTxtUsername.setText(null);
        authenticator.mTxtPassword.setText(null);

        assertThat(authenticator.areAllInputsNotEmpty()).isFalse();
    }

    @Test
    public void areInputsNotEmpty_oneEmpty_returnsEmpty() {
        authenticator.mTxtUsername.setText("username");
        authenticator.mTxtPassword.setText("");

        assertThat(authenticator.areAllInputsNotEmpty()).isFalse();
    }

    @Test
    public void loginButtonEnabledState_emptyInputs_isDisabled() {
        authenticator.mTxtUsername.setText("");
        authenticator.mTxtPassword.setText("");

        assertThat(authenticator.mBtnLogin.isEnabled()).isFalse();
    }

    @Test
    public void loginButtonEnabledState_nonEmptyInputs_isEnabled() {
        authenticator.mTxtUsername.setText("edtUsername");
        authenticator.mTxtPassword.setText("password");

        assertThat(authenticator.mBtnLogin.isEnabled()).isTrue();
    }

    @Test
    @Config(sdk = 27)
    public void onCreate_withPresetUsername_disablesUsernameInput() {
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, "username123");
        authenticator = Robolectric.buildActivity(AuthenticatorActivity.class, intent)
                .setup()
                .get();

        assertThat(authenticator.mTxtUsername.getText().toString()).isEqualTo("username123");
        assertThat(authenticator.mTxtUsername.isEnabled()).isFalse();
        assertThat(authenticator.mTxtPassword.hasFocus()).isTrue();
    }

    @Test
    public void testSuccessfulLogin() {
        final String username = "username";
        final String password = "password";
        final AuthToken authToken = new AuthToken("authTokenName", "authTokenId");

        authenticator.mAuthenticator = mock(fi.bitrite.android.ws.auth.Authenticator.class);
        when(authenticator.mAuthenticator.login(
                new Account(username, MOCK_ACCOUNT_TYPE),
                password,
                false))
                .thenReturn(Observable.just(Authenticator.AuthResult.success(authToken)));

        authenticator.mTxtUsername.setText(username);
        authenticator.mTxtPassword.setText(password);
        authenticator.mCkbRememberPassword.setChecked(false);
        authenticator.mBtnLogin.performClick();

        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();

        assertThat(authenticator.isFinishing()).isTrue();
    }
    
    @Test
    public void testFailedLogin() {
        final String username = "username";
        final String password = "password";

        authenticator.mAuthenticator = mock(fi.bitrite.android.ws.auth.Authenticator.class);
        when(authenticator.mAuthenticator.login(
                new Account(username, MOCK_ACCOUNT_TYPE),
                password,
                false))
                .thenReturn(Observable.just(Authenticator.AuthResult.error("Some error")));

        authenticator.mTxtUsername.setText(username);
        authenticator.mTxtPassword.setText(password);
        authenticator.mCkbRememberPassword.setChecked(false);
        authenticator.mBtnLogin.performClick();

        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();

        assertThat(authenticator.isFinishing()).isFalse();
    }
}