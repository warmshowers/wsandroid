package fi.bitrite.android.ws.auth;

import android.accounts.Account;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.di.DaggerTestAppComponent;
import fi.bitrite.android.ws.di.TestAppComponent;
import fi.bitrite.android.ws.di.account.AccountComponentManager;
import fi.bitrite.android.ws.di.account.TestAccountComponent;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.shadow.ShadowAccountManager;
import io.reactivex.Observable;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(shadows={ShadowAccountManager.class})
public class AuthTest {
    @Inject android.accounts.AccountManager mAndroidAccountManager;
    @Inject Authenticator mAuthenticator;
    @Inject MockWebServer mMockWebServer;
    @Inject UserRepository mUserRepository;

    private TestAppComponent mAppComponent;
    private AccountManager mAccountManager;
    private ShadowAccountManager mShadowAccountManager;

    @Before
    public void setup() {
        WSAndroidApplication app = (WSAndroidApplication) RuntimeEnvironment.application;

        mAppComponent = DaggerTestAppComponent.builder()
                .application(app)
                .build();
        mAppComponent.inject(app);

        mAccountManager = mAppComponent.getAccountManager();
    }

    @Test
    @Config(sdk = 23)
    public void testAuthTokenExpiryWithAutoRelogin() {
        final int loginUserId = 1000;
        final String username = "test";
        final String password = "very_secret";

        // Create an account but and store the password in Android's account manager.
        final AuthData authData = new AuthData(new Account(username, BuildConfig.ACCOUNT_TYPE),
                                               new AuthToken("SSESStest", "oldAuthTokenId"),
                                               "oldCsrfToken");
        createAccount(authData, loginUserId, password);

        mShadowAccountManager.setGetAuthTokenCallback(bundle -> {
            // Since we saved the password in Android's account manager the {@link Authenticator}
            // automatically attempted a re-login and could renew the new authToken.
            String authToken = bundle.getString(android.accounts.AccountManager.KEY_AUTHTOKEN);
            assertThat(authToken).isNotEmpty(); // The authenticator should automatically re-auth.
            return authToken;
        });

        testAuthTokenExpiry(authData, loginUserId, password);
    }

    @Test
    @Config(sdk = 23)
    public void testAuthTokenExpiryWithoutAutoRelogin() {
        final int loginUserId = 2000;
        final String username = "test2";
        final String password = "very_secret2";

        // Create an account but do not store any password in Android's account manager.
        final AuthData authData = new AuthData(new Account(username, BuildConfig.ACCOUNT_TYPE),
                                               new AuthToken("SSESStest", "oldAuthTokenId"),
                                               "oldCsrfToken");
        createAccount(authData, loginUserId, null);

        mShadowAccountManager.setGetAuthTokenCallback(bundle -> {
            // Since we did not save any password in Android's account manager we mock a re-login in
            // the {@link AuthenticatorActivity} by the user.
            String authToken = bundle.getString(android.accounts.AccountManager.KEY_AUTHTOKEN);
            assertThat(authToken).isNullOrEmpty();

            final Account account = authData.account;
            Intent intent = bundle.getParcelable(android.accounts.AccountManager.KEY_INTENT);
            assertThat(intent).isNotNull();
            assertThat(intent.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME))
                    .isEqualTo(account.name);
            assertThat(intent.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_TYPE))
                    .isEqualTo(account.type);
            assertThat(intent.hasExtra(android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE))
                    .isTrue();

            Authenticator.AuthResult authResult = mAuthenticator.login(account, password, false)
                    .blockingFirst();
            assertThat(authResult).isNotNull();
            return authResult.authToken.toString();
        });

        testAuthTokenExpiry(authData, loginUserId, password);
    }

    private void testAuthTokenExpiry(final AuthData authData, int loginUserId, String password) {
        final String username = authData.account.name;
        final int fetchUserId = loginUserId+1;

        mMockWebServer.setDispatcher(new Dispatcher() {
            private int mStep = 0;

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                try {
                    if (request.getPath().equals("/services/rest/user/" + loginUserId)) {
                        return createOkResponse("result_get_user_ok.json",
                                                "${userId}", loginUserId);
                        // The logged in user is fetched, too.
                    }

                    switch (mStep++) {
                        case 0: {
                            // The first user fetch reports a authToken timeout.
                            assertThat(request.getPath()).isEqualTo( "/services/rest/user/" + fetchUserId);
                            assertThat(request.getHeader("Cookie")).contains("SESStest=oldAuthTokenId");
                            return new MockResponse()
                                    .setStatus("HTTP/1.1 403 : Access denied for user anonymous");
                        }

                        case 1: {
                            // The second request is the automatic re-login attempt.
                            String requestBody = request.getBody().toString();
                            assertThat(request.getPath()).isEqualTo("/services/rest/user/login");
                            assertThat(requestBody).contains("username=" + username);
                            assertThat(requestBody).contains("password=" + password);

                            return createOkResponse("result_login_ok.json",
                                                    "${userId}", loginUserId);
                        }

                        case 2: {
                            // The third request is the second attempt of the user fetch.
                            assertThat(request.getPath()).isEqualTo("/services/rest/user/" + fetchUserId);
                            assertThat(request.getHeader("Cookie")).contains("SESStest=newAuthTokenId");

                            return createOkResponse("result_get_user_ok.json",
                                                    "${userId}", fetchUserId);
                        }
                    }
                } catch (Exception e) {
                }
                return null;
            }
        });

        Observable<Resource<User>> userRx = mUserRepository.get(fetchUserId)
                .filter(userResource -> {
                    assertThat(userResource.isError()).isFalse();
                    return userResource.isSuccess();
                });
        Resource<User> user = null;
        for (int i = 0; i <= 30; ++i) {
            try {
                user = userRx.timeout(1, TimeUnit.SECONDS)
                        .doOnError(e -> {
                            assertThat(e).isInstanceOf(TimeoutException.class);
                            Robolectric.flushBackgroundThreadScheduler();
                            Robolectric.flushForegroundThreadScheduler();
                        })
                        .blockingFirst();
            } catch (Throwable e) {}
        }
        assertThat(user).isNotNull();
        assertThat(user.isSuccess()).isTrue();
        assertThat(user.data.id).isEqualTo(fetchUserId);
    }

    private void createAccount(AuthData authData, int userId, @Nullable String password) {
        mAccountManager.updateOrCreateAccount(authData, userId, password);

        AccountComponentManager accountComponentManager = mAppComponent.getAccountComponentManager();
        TestAccountComponent accountComponent =
                (TestAccountComponent) accountComponentManager.get(authData.account);
        accountComponent.inject(this);

        mShadowAccountManager = Shadow.extract(mAndroidAccountManager);
        mShadowAccountManager.setAccountAuthenticator(mAuthenticator);
    }


    private MockResponse createOkResponse(String filename, Object... keyValues) throws  IOException {
        assert keyValues.length % 2 == 0;

        String responseBody = readFile(getClass().getClassLoader().getResource(filename).getPath());
        for (int i = 0; i < keyValues.length; i+=2) {
            responseBody = responseBody.replace(keyValues[i].toString(), keyValues[i+1].toString());
        }
        return new MockResponse()
                .setBody(responseBody);
    }

    static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
