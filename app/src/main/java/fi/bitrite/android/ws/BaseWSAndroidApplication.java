package fi.bitrite.android.ws;

import android.app.Activity;
import android.app.Application;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import fi.bitrite.android.ws.di.AppComponent;
import fi.bitrite.android.ws.di.AppInjector;

public abstract class BaseWSAndroidApplication extends Application implements HasActivityInjector {

    public static final String TAG = "WSAndroidApplication";
    private static AppInjector mAppInjector;

    @Inject DispatchingAndroidInjector<Activity> mDispatchingAndroidInjector;

    public static AppComponent getAppComponent() {
        return mAppInjector.getAppComponent();
    }

    public void onCreate() {
        super.onCreate();

        mAppInjector = inject();
        // Injected variables are available from this point.
    }

    protected abstract AppInjector inject();

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return mDispatchingAndroidInjector;
    }
}
