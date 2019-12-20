package fi.bitrite.android.ws.di;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import fi.bitrite.android.ws.api.MockServiceFactory;
import fi.bitrite.android.ws.api.ServiceFactory;
import fi.bitrite.android.ws.api.WarmshowersWebservice;
import fi.bitrite.android.ws.api.interceptors.DefaultInterceptor;
import okhttp3.mockwebserver.MockWebServer;

@Module
public class MockWebserviceModule {
    private final MockWebServer mMockWebServer = new MockWebServer();

    @Provides
    MockWebServer provideMockWebServer() {
        return mMockWebServer;
    }

    @Provides
    ServiceFactory provideServiceFactory(@Named("WSBaseUrl") String baseUrl,
                                         DefaultInterceptor defaultInterceptor) {
        return new MockServiceFactory(baseUrl, defaultInterceptor);
    }

    @Provides
    @Named("WSBaseUrl")
    String provideWSBaseUrl(MockWebServer mockWebServer) {
        return mockWebServer.url("/").toString();
    }

    @Provides
    WarmshowersWebservice provideWarmshowersWebservice(ServiceFactory serviceFactory) {
        return serviceFactory.createWarmshowersWebservice();
    }

    @Provides
    AppComponent provideAppComponent(TestAppComponent component) {
        return component;
    }
}
