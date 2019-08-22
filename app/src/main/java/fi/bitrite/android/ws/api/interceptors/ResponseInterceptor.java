package fi.bitrite.android.ws.api.interceptors;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.auth.AuthToken;
import fi.bitrite.android.ws.di.account.AccountScope;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@AccountScope
public class ResponseInterceptor implements Interceptor {

    public interface Handler {
        /**
         * Requests a new CSRF token from the API endpoint.
         * @return True, iff a new CSRF token was optained.
         */
        boolean handleCsrfValidationError();

        /**
         * Invalidates the current auth token and re-authenticates the user to obtain a new token
         * from the API endpoint.
         * @param oldAuthToken The now deprecated auth token
         * @return True, iff a new auth token was optained.
         */
        boolean handleAuthTokenExpiration(AuthToken oldAuthToken);

        /**
         * Waits until the auth token becomes available.
         * @return True, iff no error occured.
         */
        boolean waitForAuthToken();
    }

    private Handler handler;

    @Inject
    public ResponseInterceptor() {
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        return handleResponse(chain, response, 0);
    }

    private static String parseBodyStr(String rawBody) throws IOException {
        if (rawBody.startsWith("[\"") && rawBody.endsWith("\"]")) {
            return rawBody.substring(2, rawBody.length()-2);
        }
        return rawBody;
    }

    private Response handleResponse(Chain chain, Response response, int lastResponseCode)
            throws IOException {
        // No handler, no error resolving.
        if (handler == null) {
            return response;
        }

        // If we get the same error twice we give up.
        if (response.code() == lastResponseCode) {
            return response;
        }

        boolean errorResolvingAttemptDone = false;

        final ResponseBody body = response.body();
        String rawBodyStr = null;

        // Listen for auth-related error responses.
        switch (response.code()) {
            case 401: {
                // Wrong username or password. --> Happens only during login -> ignore.
                // CSRF validation failed      --> Request a new CSRF token from API endpoint.
                // API key related errors      --> Don't handle here.
                rawBodyStr = body != null ? body.string() : "";
                String parsedBodyStr = parseBodyStr(rawBodyStr);
                if (!"CSRF validation failed".equals(parsedBodyStr)) {
                    break;
                }

                // Requests a new CSRF token from the API endpoint.
                errorResolvingAttemptDone = handler.handleCsrfValidationError();

                break;
            }

            case 403: {
                // Access denied for user anonymous --> auth token timed out -> re-login
                rawBodyStr = body != null ? body.string() : "";
                String parsedBodyStr = parseBodyStr(rawBodyStr);
                if (!parsedBodyStr.startsWith("Access denied for user anonymous")) {
                    // We simply do not have access for the resource.
                    break;
                }

                // Checks if the auth-headers are set.
                List<String> cookies = response.request().headers("Cookie");
                // There is only one Cookie header (required by RFC).
                String cookieStr = cookies.isEmpty() ? "" : (cookies.get(0)+";");
                int authTokenStart = cookieStr.indexOf("SSESS");
                if (authTokenStart > -1) {
                    // We provided the authToken but it was not accepted.
                    int authTokenEnd = cookieStr.indexOf(';', authTokenStart);
                    String authTokenStr = cookieStr.substring(authTokenStart, authTokenEnd);
                    // Copies the authToken from the cookie header. Above we ensure that it ends
                    // with a ';'.
                    AuthToken authToken = AuthToken.fromString(authTokenStr);
                    errorResolvingAttemptDone = handler.handleAuthTokenExpiration(authToken);
                } else {
                    // We did not provide any authToken -> waits for it to become available.
                    errorResolvingAttemptDone = handler.waitForAuthToken();
                }

                break;
            }
        }

        if (!errorResolvingAttemptDone) {
            if (body != null && rawBodyStr != null) {
                response = response.newBuilder()
                        .body(ResponseBody.create(body.contentType(), rawBodyStr))
                        .build();
            }
            return response;
        }

        // Retries the initial call.
        lastResponseCode = response.code();

        Request request = chain.request();
        response = chain.proceed(request);
        return handleResponse(chain, response, lastResponseCode);
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }
}
