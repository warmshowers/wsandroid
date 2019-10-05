package fi.bitrite.android.ws.shadow;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.os.Parcel;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.function.Function;

import fi.bitrite.android.ws.auth.Authenticator;

@Implements(AccountManager.class)
public class ShadowAccountManager extends org.robolectric.shadows.ShadowAccountManager {

    private Authenticator mAuthenticator;
    private Function<Bundle, String> mGetAuthTokenCallback;

    public void setAccountAuthenticator(Authenticator authenticator) {
        mAuthenticator = authenticator;
    }

    public void setGetAuthTokenCallback(Function<Bundle, String> getAuthTokenCallback) {
        mGetAuthTokenCallback = getAuthTokenCallback;
    }

    @Implementation
    protected String blockingGetAuthToken(
            Account account, String authTokenType, boolean notifyAuthFailure) {
        String authToken = super.blockingGetAuthToken(account, authTokenType, notifyAuthFailure);
        if(authToken != null) {
            return authToken;
        }

        if (mAuthenticator == null) {
            return null;
        }

        Parcel parcel = Parcel.obtain();
        AccountAuthenticatorResponse response = new AccountAuthenticatorResponse(parcel);
        Bundle result = mAuthenticator.getAuthToken(response, account, authTokenType, null);
        parcel.recycle();

        if (mGetAuthTokenCallback != null) {
            return mGetAuthTokenCallback.apply(result);
        }

        return null;
    }
}
