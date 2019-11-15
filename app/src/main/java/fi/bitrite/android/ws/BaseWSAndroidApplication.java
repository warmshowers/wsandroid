package fi.bitrite.android.ws;

import android.content.SharedPreferences;

import com.bumptech.glide.request.RequestOptions;
import com.u.securekeys.SecureEnvironment;

import javax.inject.Inject;

import androidx.multidex.MultiDexApplication;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import fi.bitrite.android.ws.di.AppComponent;
import fi.bitrite.android.ws.di.AppInjector;
import fi.bitrite.android.ws.repository.SettingsRepository;
import fi.bitrite.android.ws.util.WSGlide;

public abstract class BaseWSAndroidApplication extends MultiDexApplication implements HasAndroidInjector {

    public static final String TAG = "WSAndroidApplication";
    private static AppInjector mAppInjector;

    @Inject DispatchingAndroidInjector<Object> mDispatchingAndroidInjector;
    @Inject SettingsRepository mSettingsRepository;

    public static AppComponent getAppComponent() {
        return mAppInjector.getAppComponent();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SecureEnvironment.initialize(this);

        mAppInjector = inject();
        // Injected variables are available from this point.

        mSettingsRepository.registerOnChangeListener(mSettingsChangeListener);
    }

    @Override
    public void onTerminate() {
        mSettingsRepository.unregisterOnChangeListener(mSettingsChangeListener);
        super.onTerminate();
    }

    protected abstract AppInjector inject();

    @Override
    public AndroidInjector<Object> androidInjector() {
        return mDispatchingAndroidInjector;
    }

    // Ensure that pictures are not loaded in data-saver mode.
    private final SharedPreferences.OnSharedPreferenceChangeListener mSettingsChangeListener =
            (sharedPreferences, key) -> {
                RequestOptions requestOptions = new RequestOptions()
                        .onlyRetrieveFromCache(mSettingsRepository.isDataSaverMode());
                WSGlide.setDefaultRequestOptions(requestOptions);
            };
}
