package fi.bitrite.android.ws.auth;

import android.accounts.Account;

public class AuthData {
    public final Account account;
    public final AuthToken authToken;
    public final String csrfToken;

    public AuthData() {
        this(null, null, null);
    }

    public AuthData(Account account, AuthToken authToken, String csrfToken) {
        this.account = account;
        this.authToken = authToken;
        this.csrfToken = csrfToken;
    }

    public boolean isValid() {
        return account != null
               && authToken != null
               && csrfToken != null;
    }
}
