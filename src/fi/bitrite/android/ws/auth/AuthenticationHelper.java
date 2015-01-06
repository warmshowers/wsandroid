package fi.bitrite.android.ws.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;

import java.io.IOException;

import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.util.GlobalInfo;

public class AuthenticationHelper {
    public static final String KEY_SESSION_NAME = "session_name";
    public static final String KEY_SESSID = "sessid";
    public static final String KEY_USERID = "userid";

    public static Account createNewAccount(String username, String password) {
        AccountManager accountManager = AccountManager.get(WSAndroidApplication.getAppContext());
        Account account = new Account(username, AuthenticationService.ACCOUNT_TYPE);
        accountManager.addAccountExplicitly(account, null, null);
        accountManager.setAuthToken(account, AuthenticationService.ACCOUNT_TYPE, password);
        return account;
    }

    public static Account getWarmshowersAccount() throws NoAccountException {
        AccountManager accountManager = AccountManager.get(WSAndroidApplication.getAppContext());
        Account[] accounts = accountManager.getAccountsByType(AuthenticationService.ACCOUNT_TYPE);

        if (accounts.length == 0) {
            throw new NoAccountException();
        }

        return accounts[0];
    }

    public static String getAccountCookie() {
        AccountManager accountManager = AccountManager.get(WSAndroidApplication.getAppContext());
        Account account = getWarmshowersAccount();
        String session_name = accountManager.getUserData(account, KEY_SESSION_NAME);
        String sessid = accountManager.getUserData(account, KEY_SESSID);
        String cookieString = session_name + "=" + sessid + "; domain=" + GlobalInfo.warmshowersCookieDomain;
        return cookieString;
    }

    public static int getAccountUid() {
        AccountManager accountManager = AccountManager.get(WSAndroidApplication.getAppContext());
        Account account = getWarmshowersAccount();
        String sUid = accountManager.getUserData(account, KEY_USERID);
        int uid = -1;
        try {
            uid = Integer.parseInt(sUid);
        } catch (NumberFormatException e) {
            // Ignore, and we'll go with -1 for the uid. Doesn't work well, but cheater.
            // This probably should only be happening on initial 1.4.1 upgrade where account didn't have
            // the UID stashed in it.
        }
        return uid;
    }

    public static String getAccountUsername() {
        Account account = getWarmshowersAccount();
        String username = account.name;
        return username;
    }

    public static String getAccountPassword() throws OperationCanceledException, IOException, AuthenticatorException {
        AccountManager accountManager = AccountManager.get(WSAndroidApplication.getAppContext());
        Account account = getWarmshowersAccount();

        // authToken is here instead of password for legacy reasons, and doesn't make sense to
        // break all existing accounts by changing it.
        String password = accountManager.blockingGetAuthToken(account, AuthenticationService.ACCOUNT_TYPE, true);
        return password;
    }

    public static void addCookieInfo(String cookieSessionName, String cookieSessionId, int userId) {
        AccountManager accountManager = AccountManager.get(WSAndroidApplication.getAppContext());
        Account account = getWarmshowersAccount();

        accountManager.setUserData(account, KEY_SESSION_NAME, cookieSessionName);
        accountManager.setUserData(account, KEY_SESSID, cookieSessionId);
        accountManager.setUserData(account, KEY_USERID, String.valueOf(userId));
    }
}
