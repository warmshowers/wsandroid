package fi.bitrite.android.ws.api_new;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;

import fi.bitrite.android.ws.api_new.deserializer.BooleanDeserializer;
import fi.bitrite.android.ws.api_new.deserializer.DateDeserializer;
import fi.bitrite.android.ws.api_new.interceptors.DefaultInterceptor;
import fi.bitrite.android.ws.api_new.interceptors.HeaderInterceptor;
import fi.bitrite.android.ws.api_new.interceptors.ResponseInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceFactory {

    private ServiceFactory() {}

    public static WarmshowersService createWarmshowersService(
            DefaultInterceptor defaultInterceptor, HeaderInterceptor headerInterceptor,
            ResponseInterceptor responseInterceptor) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                // They must be in correct order.
                .addInterceptor(defaultInterceptor)
                .addInterceptor(responseInterceptor)
                .addInterceptor(headerInterceptor);

        final BooleanDeserializer booleanDeserializer = new BooleanDeserializer();
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(Boolean.class, booleanDeserializer)
                .registerTypeAdapter(boolean.class, booleanDeserializer)
                .create();

        return new Retrofit.Builder()
                .baseUrl("https://www.warmshowers.org/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new StringConverterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(clientBuilder.build())
                .build()
                .create(WarmshowersService.class);
    }
}
