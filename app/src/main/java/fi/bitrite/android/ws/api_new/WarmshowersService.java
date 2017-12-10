package fi.bitrite.android.ws.api_new;

import fi.bitrite.android.ws.api_new.response.LoginResponse;
import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface WarmshowersService {

    @POST("services/rest/user/login")
    @FormUrlEncoded
    Single<Response<LoginResponse>> login(@Field("username") String username,
                                          @Field("password") String password);

    @GET("services/session/token")
    Single<Response<String>> renewCsrfToken();
}
