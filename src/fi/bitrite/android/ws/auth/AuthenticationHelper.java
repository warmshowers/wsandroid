package fi.bitrite.android.ws.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import fi.bitrite.android.ws.WSAndroidApplication;

public class AuthenticationHelper {

	public static Account getWarmshowersAccount() {
		AccountManager accountManager = AccountManager.get(WSAndroidApplication.getAppContext());
		Account[] accounts = accountManager.getAccountsByType("org.warmshowers");
		
		if (accounts.length == 0) {
			throw new NoAccountException();
		}
		
		return accounts[0];
	}

}
