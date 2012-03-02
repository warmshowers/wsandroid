package fi.bitrite.android.ws.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AuthenticationService extends Service {
	
	public static final String ACCOUNT_TYPE = "org.warmshowers";

	private static final String TAG = "WSAndroid";
	private Authenticator mAuthenticator;

	@Override
	public void onCreate() {
		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.v(TAG, "WSAndroid Authentication Service started.");
		}
		mAuthenticator = new Authenticator(this);
	}

	@Override
	public void onDestroy() {
		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.v(TAG, "WSAndroid Authentication Service stopped.");
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.v(TAG, "getBinder()...  returning the WSAndroid binder for intent " + intent);
		}
		return mAuthenticator.getIBinder();
	}
}
