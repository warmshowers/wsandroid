package fi.bitrite.android.ws.api.interceptors;

import android.os.Build;

import com.u.securekeys.SecureEnvironment;
import com.u.securekeys.annotation.SecureKey;
import com.u.securekeys.annotation.SecureKeys;

import java.io.IOException;

import javax.inject.Inject;

import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.di.AppScope;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@AppScope
public class DefaultInterceptor implements Interceptor {

    private static final String USER_AGENT = String.format("WSAndroid %s %s %s Android v%s",
            BuildConfig.VERSION_NAME, Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE);

    @Inject
    public DefaultInterceptor() {
    }

    @Override
    @SecureKeys({
        @SecureKey(key = "ws_api_userId", value = BuildConfig.WS_API_USER_ID),
        @SecureKey(key = "ws_api_key", value = BuildConfig.WS_API_KEY)
    })
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        // Construct the API key credential.
        String credential = Credentials.basic(
                SecureEnvironment.getString("ws_api_userId"),
                SecureEnvironment.getString("ws_api_key"));

        Request request = original.newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Authorization", credential)
                .build();

        return chain.proceed(request);
    }
}
