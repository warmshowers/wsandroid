package fi.bitrite.android.ws.di;

import com.u.securekeys.SecureEnvironment;
import com.u.securekeys.annotation.SecureKey;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.api.ServiceFactory;
import fi.bitrite.android.ws.api.WarmshowersWebservice;
import fi.bitrite.android.ws.api.interceptors.DefaultInterceptor;

@Module
public class WebserviceModule {
    @Provides
    @Named("WSBaseUrl")
    @SecureKey(key = "ws_base_url", value = BuildConfig.WS_BASE_URL)
    String provideWSBaseUrl() {
        return SecureEnvironment.getString("ws_base_url");
    }

    @Provides
    ServiceFactory provideServiceFactory(@Named("WSBaseUrl") String baseUrl,
                                         DefaultInterceptor defaultInterceptor) {
        return new ServiceFactory(baseUrl, defaultInterceptor);
    }

    @Provides
    WarmshowersWebservice provideWarmshowersWebservice(ServiceFactory serviceFactory) {
        return serviceFactory.createWarmshowersWebservice();
    }
}
