package fi.bitrite.android.ws.auth;

import android.accounts.Account;

import fi.bitrite.android.ws.di.account.AccountComponentManager;
import fi.bitrite.android.ws.di.account.TestAccountComponent;

/**
 * Helper class to set up mock accounts. Use through {@link fi.bitrite.android.ws.WSTest}.
 */
public class AccountHelper {
    public final static AuthData DEFAULT_AUTH_DATA = new AuthData(
            new Account("mockUser", "mockAccountType"),
            new AuthToken("mockAuthTokenName", "mockAuthTokenId"),
            "mockCsrfToken");
    public final static int DEFAULT_USERID = 1000;
    public final static String DEFAULT_PASSWORD = "verySecure";

    private final AccountManager mAccountManager;
    private final AccountComponentManager mAccountComponentManager;

    public AccountHelper(AccountManager accountManager,
                         AccountComponentManager accountComponentManager) {
        mAccountManager = accountManager;
        mAccountComponentManager = accountComponentManager;
    }

    public TestAccountComponent createAccount() {
        return createAccount(DEFAULT_AUTH_DATA, DEFAULT_USERID, DEFAULT_PASSWORD);

    }
    public TestAccountComponent createAccount(AuthData authData, int userId, String csrfToken) {
        mAccountManager.updateOrCreateAccount(authData, userId, csrfToken);
        return (TestAccountComponent) mAccountComponentManager.get(authData.account);
    }
}
