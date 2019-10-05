package fi.bitrite.android.ws.di;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
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
    @Named("WSBaseUrl")
    String provideWSBaseUrl() {
        return mMockWebServer.url("/").toString();
    }

    @Provides
    WarmshowersWebservice provideWarmshowersWebservice(@Named("WSBaseUrl") String baseUrl,
                                                       DefaultInterceptor defaultInterceptor) {
        return ServiceFactory.createWarmshowersWebservice(baseUrl, defaultInterceptor);
    }

    @Provides
    AppComponent provideAppComponent(TestAppComponent component) {
        return component;
    }
}
