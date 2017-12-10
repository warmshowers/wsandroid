package fi.bitrite.android.ws.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.api_new.ServiceFactory;
import fi.bitrite.android.ws.api_new.WarmshowersService;
import fi.bitrite.android.ws.api_new.interceptors.DefaultInterceptor;
import fi.bitrite.android.ws.api_new.interceptors.HeaderInterceptor;
import fi.bitrite.android.ws.api_new.interceptors.ResponseInterceptor;

@Module
public class ServiceModule {
    @Provides
    @Singleton
    WarmshowersService provideWarmshowersService(
            DefaultInterceptor defaultInterceptor, HeaderInterceptor headerInterceptor,
            ResponseInterceptor responseInterceptor) {
        return ServiceFactory.createWarmshowersService(
                defaultInterceptor, headerInterceptor, responseInterceptor);
    }
}
