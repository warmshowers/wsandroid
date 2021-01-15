package fi.bitrite.android.ws.api;

import androidx.annotation.NonNull;
import fi.bitrite.android.ws.api.interceptors.DefaultInterceptor;
import okhttp3.OkHttpClient;

/**
 * Turns off security measures for connections in tests.
 */
public class MockServiceFactory extends ServiceFactory {

    public MockServiceFactory(String baseUrl,
                              DefaultInterceptor defaultInterceptor) {
        super(baseUrl, defaultInterceptor);
    }

    @Override
    void setSslSocketFactory(@NonNull OkHttpClient.Builder clientBuilder) {}

    @Override
    void setConnectionSpec(@NonNull OkHttpClient.Builder clientBuilder) {}
}
