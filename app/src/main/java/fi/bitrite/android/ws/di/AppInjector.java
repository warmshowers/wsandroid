package fi.bitrite.android.ws.di;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import dagger.android.AndroidInjection;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.HasSupportFragmentInjector;
import fi.bitrite.android.ws.WSAndroidApplication;

/**
 * Helper class to automatically inject fragments if they implement {@link Injectable}.
 */
public class AppInjector {
    private final AppComponent mAppComponent;

    private AppInjector(WSAndroidApplication wsApp) {
        mAppComponent = DaggerAppComponent.builder()
                .application(wsApp)
                .build();
        mAppComponent.inject(wsApp);
    }

    public static AppInjector create(WSAndroidApplication wsApp) {
        AppInjector injector = new AppInjector(wsApp);

        wsApp.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (activity instanceof HasSupportFragmentInjector ||
                        activity instanceof Injectable) {
                    AndroidInjection.inject(activity);
                }
                if (activity instanceof FragmentActivity) {
                   FragmentManager.FragmentLifecycleCallbacks cb =
                           new FragmentManager.FragmentLifecycleCallbacks() {
                               @Override
                               public void onFragmentCreated(
                                       FragmentManager fm, Fragment f, Bundle savedInstanceState) {
                                   if (f instanceof Injectable) {
                                       AndroidSupportInjection.inject(f);
                                   }
                               }
                           };
                    ((FragmentActivity) activity).getSupportFragmentManager()
                            .registerFragmentLifecycleCallbacks(cb, true);
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {}

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });

        return injector;
    }

    public AppComponent getAppComponent() {
        return mAppComponent;
    }
}
