package fi.bitrite.android.ws.api;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;

import fi.bitrite.android.ws.api.interceptors.DefaultInterceptor;
import fi.bitrite.android.ws.api.interceptors.HeaderInterceptor;
import fi.bitrite.android.ws.api.interceptors.ResponseInterceptor;
import fi.bitrite.android.ws.api.typeadapter.BooleanDeserializer;
import fi.bitrite.android.ws.api.typeadapter.DateDeserializer;
import fi.bitrite.android.ws.api.typeadapter.RatingTypeAdapter;
import fi.bitrite.android.ws.api.typeadapter.RelationTypeAdapter;
import fi.bitrite.android.ws.model.Feedback;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceFactory {

    private ServiceFactory() {}

    public static WarmshowersWebservice createWarmshowersWebservice(
            String baseUrl, DefaultInterceptor defaultInterceptor) {
        OkHttpClient client = createDefaultClientBuilder(defaultInterceptor).build();
        Gson gson = createDefaultGsonBuilder().create();

        return createDefaultRetrofitBuilder(baseUrl, client, gson)
                .build()
                .create(WarmshowersWebservice.class);
    }

    public static WarmshowersAccountWebservice createWarmshowersAccountWebservice(
            String baseUrl, DefaultInterceptor defaultInterceptor,
            HeaderInterceptor headerInterceptor, ResponseInterceptor responseInterceptor) {
        OkHttpClient client = createDefaultClientBuilder(defaultInterceptor)
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

    private static OkHttpClient.Builder createDefaultClientBuilder(
            DefaultInterceptor defaultInterceptor) {
       return OkHttpClientProvider.createClientBuilder().addInterceptor(defaultInterceptor);
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
