package fi.bitrite.android.ws.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.auth.AuthenticationService;

@Singleton
@Component(modules = {
        AndroidInjectionModule.class,
        AppModule.class,
        ActivitiesModule.class,
        ServiceModule.class,
})
public interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application application);

        AppComponent build();
    }

    void inject(WSAndroidApplication wsApp);
    void inject(AuthenticationService service);
}
