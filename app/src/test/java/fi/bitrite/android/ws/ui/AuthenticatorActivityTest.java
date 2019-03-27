package fi.bitrite.android.ws.ui;

import android.accounts.AccountManager;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import fi.bitrite.android.ws.R;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class AuthenticatorActivityTest {

    private ActivityController<AuthenticatorActivity> controller;
    private AuthenticatorActivity authenticator;

    private EditText username;
    private EditText password;
    private Button login;


    @Before
    public void setUp() throws Exception {
        authenticator = Robolectric.setupActivity(AuthenticatorActivity.class);

        login = authenticator.findViewById(R.id.auth_btn_login);
        username = authenticator.findViewById(R.id.auth_txt_username);
        password = authenticator.findViewById(R.id.auth_txt_password);
    }

    @Test
    public void areInputsNotEmpty_empty_returnsEmpty() throws Exception {
        username.setText("");
        password.setText("");

        assertThat(authenticator.areAllInputsNotEmpty()).isFalse();
    }

    @Test
    public void areInputsNotEmpty_notEmpty_returnsNotEmpty() throws Exception {
        username.setText("username");
        password.setText("password");

        assertThat(authenticator.areAllInputsNotEmpty()).isTrue();
    }

    @Test
    public void areInputsNotEmpty_null_returnsEmpty() throws Exception {
        username.setText(null);
        password.setText(null);

        assertThat(authenticator.areAllInputsNotEmpty()).isFalse();
    }

    @Test
    public void areInputsNotEmpty_oneEmpty_returnsEmpty() throws Exception {
        username.setText("username");
        password.setText("");

        assertThat(authenticator.areAllInputsNotEmpty()).isFalse();
    }

    @Test
    public void loginButtonEnabledState_emptyInputs_isDisabled() throws Exception {
        username.setText("");
        password.setText("");

        assertThat(login.isEnabled()).isFalse();
    }

    @Test
    public void loginButtonEnabledState_nonEmptyInputs_isEnabled() throws Exception {
        username.setText("username");
        password.setText("password");

        assertThat(login.isEnabled()).isTrue();
    }

    @Test
    @Config(sdk = 27)
    public void onCreate_withPresetUsername_disablesUsernameInput() throws Exception {
        final Intent intent =
                new Intent(RuntimeEnvironment.application, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, "username123");

        final AuthenticatorActivity activity =
                Robolectric.buildActivity(AuthenticatorActivity.class, intent)
                        .create()
                        .visible()
                        .get();

        final EditText intentActivityUsername = activity.findViewById(R.id.auth_txt_username);
        final EditText intentActivityPassword = activity.findViewById(R.id.auth_txt_password);

        assertThat(intentActivityUsername.getText().toString()).isEqualTo("username123");
        assertThat(intentActivityUsername.isEnabled()).isFalse();
        assertThat(intentActivityPassword.hasFocus()).isTrue();
    }

}