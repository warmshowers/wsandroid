package fi.bitrite.android.ws.api.interceptors;

import android.text.TextUtils;

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

    private static final String USER_AGENT = "WarmshowersApp";

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

        Request.Builder requestBuilder = original.newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json");

        // Construct the API key credential.
        final String apiUserId = SecureEnvironment.getString("ws_api_userId");
        final String apiKey = SecureEnvironment.getString("ws_api_key");
        if (!TextUtils.isEmpty(apiUserId)) {
            // The apiUserId is unset in case the dev proxy server is used.
            requestBuilder.header("Authorization", Credentials.basic(apiUserId,apiKey));
        }

        return chain.proceed(requestBuilder.build());
    }
}
