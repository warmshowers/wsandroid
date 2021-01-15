package fi.bitrite.android.ws.api;

import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import androidx.annotation.NonNull;
import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.interceptors.DefaultInterceptor;
import fi.bitrite.android.ws.api.interceptors.HeaderInterceptor;
import fi.bitrite.android.ws.api.interceptors.ResponseInterceptor;
import fi.bitrite.android.ws.api.typeadapter.BooleanDeserializer;
import fi.bitrite.android.ws.api.typeadapter.DateDeserializer;
import fi.bitrite.android.ws.api.typeadapter.RatingTypeAdapter;
import fi.bitrite.android.ws.api.typeadapter.RelationTypeAdapter;
import fi.bitrite.android.ws.model.Feedback;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import okhttp3.internal.platform.Platform;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceFactory {

    final String baseUrl;
    final DefaultInterceptor defaultInterceptor;

    public ServiceFactory(String baseUrl,
                          DefaultInterceptor defaultInterceptor) {
        this.baseUrl = baseUrl;
        this.defaultInterceptor = defaultInterceptor;
    }

    public WarmshowersWebservice createWarmshowersWebservice() {
        OkHttpClient client = createDefaultClientBuilder().build();
        Gson gson = createDefaultGsonBuilder().create();

        return createDefaultRetrofitBuilder(baseUrl, client, gson)
                .build()
                .create(WarmshowersWebservice.class);
    }

    public WarmshowersAccountWebservice createWarmshowersAccountWebservice(
            HeaderInterceptor headerInterceptor, ResponseInterceptor responseInterceptor) {
        OkHttpClient client = createDefaultClientBuilder()
                // They must be in correct order.
                .addInterceptor(responseInterceptor)
                .addInterceptor(headerInterceptor)
                .build();

        Gson gson = createDefaultGsonBuilder()
                .registerTypeAdapter(Feedback.Relation.class, new RelationTypeAdapter())
                .registerTypeAdapter(Feedback.Rating.class, new RatingTypeAdapter())
                .create();

        return createDefaultRetrofitBuilder(baseUrl, client, gson)
                .build()
                .create(WarmshowersAccountWebservice.class);
    }

    OkHttpClient.Builder createDefaultClientBuilder() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(defaultInterceptor);
        if (!TextUtils.isEmpty(BuildConfig.WS_DEV_KEYSTORE)) {
            setSslSocketFactory(clientBuilder);
            setConnectionSpec(clientBuilder);
        }
        return clientBuilder;
    }
    GsonBuilder createDefaultGsonBuilder() {
        final BooleanDeserializer booleanDeserializer = new BooleanDeserializer();
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(Boolean.class, booleanDeserializer)
                .registerTypeAdapter(boolean.class, booleanDeserializer);
    }
    Retrofit.Builder createDefaultRetrofitBuilder(String baseUrl,
                                                  OkHttpClient client,
                                                  Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new StringConverterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client);
    }

    void setSslSocketFactory(@NonNull OkHttpClient.Builder clientBuilder) {
        try {
            final X509TrustManager trustManager = getTrustManager();
            final SSLContext sslContext = Platform.get().getSSLContext();
            sslContext.init(getKeyManagers(), new TrustManager[] {trustManager}, new SecureRandom());
            SSLSocketFactory sslSocketFactory = Build.VERSION.SDK_INT >= 21
                    ? sslContext.getSocketFactory()
                    : new TLSSocketFactory(sslContext.getSocketFactory()) ;
            clientBuilder.sslSocketFactory(sslSocketFactory, trustManager);
        } catch (Exception ex) {
            Log.e(WSAndroidApplication.TAG, "Error while setting the socket factory", ex);
        }
    }

    X509TrustManager getTrustManager() throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                                            + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    KeyManager[] getKeyManagers() throws Exception {
        if (TextUtils.isEmpty(BuildConfig.WS_DEV_KEYSTORE)) {
            return null;
        }

        // Install the client certificate for the dev proxy server.
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        byte[] devKeyStore = Base64.decode(BuildConfig.WS_DEV_KEYSTORE, Base64.DEFAULT);
        keyStore.load(new ByteArrayInputStream(devKeyStore), new char[0]);

        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, null);

        return keyManagerFactory.getKeyManagers();
    }

    void setConnectionSpec(@NonNull OkHttpClient.Builder clientBuilder) {
        ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .build();
        clientBuilder.connectionSpecs(Collections.singletonList(cs));
    }


    /**
     *
     * This class is needed for TLS 1.2 support on Android 4.x
     *
     * See http://blog.dev-area.net/2015/08/13/android-4-1-enable-tls-1-1-and-tls-1-2/ and
     * https://github.com/square/okhttp/issues/2372
     */
    static class TLSSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;

        TLSSocketFactory(SSLSocketFactory sslSocketFactory) {
            delegate = sslSocketFactory;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws
                                                                                       IOException {
            return enableTLSOnSocket(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return enableTLSOnSocket(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
                throws IOException {
            return enableTLSOnSocket(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return enableTLSOnSocket(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
                                   int localPort) throws IOException {
            return enableTLSOnSocket(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket enableTLSOnSocket(Socket socket) {
            if (socket instanceof SSLSocket) {
                ((SSLSocket) socket).setEnabledProtocols(
                        new String[]{ TlsVersion.TLS_1_2.javaName() });
            }
            return socket;
        }
    }
}
