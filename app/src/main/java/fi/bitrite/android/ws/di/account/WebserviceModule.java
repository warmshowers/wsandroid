package fi.bitrite.android.ws.di.account;

import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.api.ServiceFactory;
import fi.bitrite.android.ws.api.WarmshowersAccountWebservice;
import fi.bitrite.android.ws.api.interceptors.HeaderInterceptor;
import fi.bitrite.android.ws.api.interceptors.ResponseInterceptor;

@Module
public class WebserviceModule {

    @Provides
    WarmshowersAccountWebservice provideWarmshowersAccountWebservice(
            ServiceFactory serviceFactory,
            HeaderInterceptor headerInterceptor, ResponseInterceptor responseInterceptor) {
        return serviceFactory.createWarmshowersAccountWebservice(
                headerInterceptor, responseInterceptor);
    }
}
