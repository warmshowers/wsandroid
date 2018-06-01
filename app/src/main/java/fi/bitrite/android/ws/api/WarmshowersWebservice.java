package fi.bitrite.android.ws.api;

import fi.bitrite.android.ws.api.response.LoginResponse;
import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface WarmshowersWebservice {

    /// Auth

    @POST("services/rest/user/login")
    @FormUrlEncoded
    Observable<Response<LoginResponse>> login(@Field("username") String username,
                                              @Field("password") String password);

}
