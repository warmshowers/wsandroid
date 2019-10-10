package fi.bitrite.android.ws.api;


import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collections;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

/**
 *
 * This class is needed for TLS 1.2 support on Android 4.x
 *
 * See http://blog.dev-area.net/2015/08/13/android-4-1-enable-tls-1-1-and-tls-1-2/ and
 * https://github.com/square/okhttp/issues/2372
 */
class OkHttpClientProvider {

    static OkHttpClient.Builder createClientBuilder() {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        return enableTls12OnPreLollipop(client);
    }

    private static OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT >= 21) {
            return client;
        }

        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:"
                                                + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

            SSLContext sslContext = SSLContext.getInstance(TlsVersion.TLS_1_2.javaName());
            sslContext.init(null, new TrustManager[] { trustManager }, null);
            client.sslSocketFactory(
                    new TLSSocketFactory(sslContext.getSocketFactory()), trustManager);

            ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build();
            client.connectionSpecs(Collections.singletonList(cs));
        } catch (Exception exc) {
            Log.e("OkHttpClientProvider", "Error while enabling TLS 1.2", exc);
        }

        return client;
    }

    private static class TLSSocketFactory extends SSLSocketFactory {
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
