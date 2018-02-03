package fi.bitrite.android.ws.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.api.ServiceFactory;
import fi.bitrite.android.ws.api.WarmshowersService;
import fi.bitrite.android.ws.api.interceptors.DefaultInterceptor;
import fi.bitrite.android.ws.api.interceptors.HeaderInterceptor;
import fi.bitrite.android.ws.api.interceptors.ResponseInterceptor;

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
