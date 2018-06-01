package fi.bitrite.android.ws.di;

import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.api.ServiceFactory;
import fi.bitrite.android.ws.api.WarmshowersWebservice;
import fi.bitrite.android.ws.api.interceptors.DefaultInterceptor;

@Module
public class WebserviceModule {
    @Provides
    WarmshowersWebservice provideWarmshowersWebservice(DefaultInterceptor defaultInterceptor) {
        return ServiceFactory.createWarmshowersWebservice(defaultInterceptor);
    }
}
