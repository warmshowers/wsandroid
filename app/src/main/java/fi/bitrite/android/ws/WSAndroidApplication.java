package fi.bitrite.android.ws;

import android.app.Application;
import android.util.Log;

public class WSAndroidApplication extends Application {
    private final static String TAG = WSAndroidApplication.class.getCanonicalName();

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Application create");
    }
}
