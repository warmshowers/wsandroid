package fi.bitrite.android.ws.api.interceptors;

import android.os.Build;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.BuildConfig;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@Singleton
public class DefaultInterceptor implements Interceptor {

    private static final String USER_AGENT = String.format("WSAndroid %s %s %s Android v%s",
            BuildConfig.VERSION_NAME, Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE);

    @Inject
    public DefaultInterceptor() {
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        Request request = original.newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build();

        return chain.proceed(request);
    }
}
