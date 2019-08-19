package fi.bitrite.android.ws.api;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.u.securekeys.SecureEnvironment;
import com.u.securekeys.annotation.SecureKey;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.api.interceptors.DefaultInterceptor;
import fi.bitrite.android.ws.api.interceptors.HeaderInterceptor;
import fi.bitrite.android.ws.api.interceptors.ResponseInterceptor;
import fi.bitrite.android.ws.api.typeadapter.BooleanDeserializer;
import fi.bitrite.android.ws.api.typeadapter.DateDeserializer;
import fi.bitrite.android.ws.api.typeadapter.RatingTypeAdapter;
import fi.bitrite.android.ws.api.typeadapter.RelationTypeAdapter;
import fi.bitrite.android.ws.model.Feedback;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceFactory {

    private ServiceFactory() {}

    public static WarmshowersWebservice createWarmshowersWebservice(
            String baseUrl, DefaultInterceptor defaultInterceptor) {
        OkHttpClient client = createDefaultClientBuilder(baseUrl, defaultInterceptor).build();
        Gson gson = createDefaultGsonBuilder().create();

        return createDefaultRetrofitBuilder(baseUrl, client, gson)
                .build()
                .create(WarmshowersWebservice.class);
    }

    public static WarmshowersAccountWebservice createWarmshowersAccountWebservice(
            String baseUrl, DefaultInterceptor defaultInterceptor,
            HeaderInterceptor headerInterceptor, ResponseInterceptor responseInterceptor) {
        OkHttpClient client = createDefaultClientBuilder(baseUrl, defaultInterceptor)
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

    @SecureKey(key = "ws_cert_pin", value = BuildConfig.WS_CERTIFICATE_PIN)
    private static OkHttpClient.Builder createDefaultClientBuilder(
            String baseUrl, DefaultInterceptor defaultInterceptor) {
        CertificatePinner.Builder certificatePinnerBuilder = new CertificatePinner.Builder();
        try {
            final String wsHost = new URL(baseUrl).getHost();
            final String wsPin = SecureEnvironment.getString("ws_cert_pin");
            certificatePinnerBuilder.add(wsHost, wsPin);
            certificatePinnerBuilder.add("warmshowers.org", wsPin);
            certificatePinnerBuilder.add("*.warmshowers.org", wsPin);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return OkHttpClientProvider.createClientBuilder()
                .addInterceptor(defaultInterceptor)
                .certificatePinner(certificatePinnerBuilder.build());
    }
    private static GsonBuilder createDefaultGsonBuilder() {
        final BooleanDeserializer booleanDeserializer = new BooleanDeserializer();
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(Boolean.class, booleanDeserializer)
                .registerTypeAdapter(boolean.class, booleanDeserializer);
    }
    private static Retrofit.Builder createDefaultRetrofitBuilder(String baseUrl,
                                                                 OkHttpClient client,
                                                                 Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new StringConverterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client);
    }
}
