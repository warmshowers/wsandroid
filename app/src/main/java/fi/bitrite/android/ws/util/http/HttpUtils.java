package fi.bitrite.android.ws.util.http;

import android.os.Build;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import fi.bitrite.android.ws.BuildConfig;

public class HttpUtils {

    private static final int TIMEOUT_MS = 20000;

    public static String encodeUrl(String urlString) throws MalformedURLException, URISyntaxException {
        URL url = new URL(urlString);
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        url = uri.toURL();
        return url.toString();
    }

    public static HttpClient getDefaultClient() {
        // Register http and https sockets
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", new PlainSocketFactory(), 80));
        registry.register(new Scheme("https", new TlsSniSocketFactory(), 443));

        // Standard parameters
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MS);

        String userAgentString = new StringBuilder().
                append("WSAndroid ").append(BuildConfig.VERSION_NAME).append(" ")
                .append(Build.MANUFACTURER).append(" ")
                .append(Build.MODEL).append(" ")
                .append("Android v").append(Build.VERSION.RELEASE)
                .toString();
        httpParams.setParameter(HttpProtocolParams.USER_AGENT, userAgentString);

        DefaultHttpClient httpClient = new DefaultHttpClient(
                new ThreadSafeClientConnManager(httpParams, registry), httpParams);

        return httpClient;
    }

}
