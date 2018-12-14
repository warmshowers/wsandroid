package fi.bitrite.android.ws.di.account;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.api.ServiceFactory;
import fi.bitrite.android.ws.api.WarmshowersAccountWebservice;
import fi.bitrite.android.ws.api.interceptors.DefaultInterceptor;
import fi.bitrite.android.ws.api.interceptors.HeaderInterceptor;
import fi.bitrite.android.ws.api.interceptors.ResponseInterceptor;

@Module
public class WebserviceModule {
    @Provides
    WarmshowersAccountWebservice provideWarmshowersAccountWebservice(
            @Named("WSBaseUrl") String baseUrl, DefaultInterceptor defaultInterceptor,
            HeaderInterceptor headerInterceptor, ResponseInterceptor responseInterceptor) {
        return ServiceFactory.createWarmshowersAccountWebservice(
                baseUrl, defaultInterceptor, headerInterceptor, responseInterceptor);
    }
}
