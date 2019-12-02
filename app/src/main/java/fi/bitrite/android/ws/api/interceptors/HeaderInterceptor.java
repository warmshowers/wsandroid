package fi.bitrite.android.ws.api.interceptors;

import android.text.TextUtils;

import java.io.IOException;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;

import fi.bitrite.android.ws.di.account.AccountScope;
import okhttp3.Cookie;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@AccountScope
public class HeaderInterceptor implements Interceptor {
    private final String wsDomainName;

    private String sessionCookie;
    private String csrfToken;

    @Inject
    public HeaderInterceptor(@Named("WSBaseUrl") String wsBaseUrl) {
        try {
            wsDomainName = new URL(wsBaseUrl).getAuthority();
        } catch (Exception e) {
            throw new RuntimeException("Invalid WSBaseUrl: " + e.getMessage());
        }
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        // Adds the headers to the request.
        Request.Builder builder = original.newBuilder();
        if (!TextUtils.isEmpty(sessionCookie)) {
            builder.header("Cookie", sessionCookie);
        }
        if ("POST".equals(original.method()) && !TextUtils.isEmpty(csrfToken)) {
            builder.header("X-CSRF-Token", csrfToken);
        }
        Request request = builder.build();

        return chain.proceed(request);
    }

    public void setSessionCookie(String sessionName, String sessionId) {
        sessionCookie = new Cookie.Builder()
                .name(sessionName)
                .value(sessionId)
                .domain(wsDomainName)
                .build()
                .toString();
    }
    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }
}
