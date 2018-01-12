package fi.bitrite.android.ws.api_new;

import fi.bitrite.android.ws.api_new.model.ApiUser;
import fi.bitrite.android.ws.api_new.response.FeedbackResponse;
import fi.bitrite.android.ws.api_new.response.LoginResponse;
import fi.bitrite.android.ws.api_new.response.UserSearchByLocationResponse;
import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface WarmshowersService {

    @POST("services/rest/user/login")
    @FormUrlEncoded
    Observable<Response<LoginResponse>> login(@Field("username") String username,
                                              @Field("password") String password);

    @GET("services/session/token")
    Observable<Response<String>> renewCsrfToken();

    @GET("services/rest/user/{userId}")
    Observable<Response<ApiUser>> fetchUser(@Path("userId") int userId);

    @GET("user/{userId}/json_recommendations")
    Observable<Response<FeedbackResponse>> fetchFeedbackForRecipient(@Path("userId") int recipientId);

    public final static int SEARCH_USER_DEFAULT_LIMIT = 800;

    @POST("/services/rest/hosts/by_location")
    @FormUrlEncoded
    Observable<Response<UserSearchByLocationResponse>> searchUsersByLocation(
            @Field("minlat") double minLat, @Field("minlon") double minLon,
            @Field("maxlat") double maxLat, @Field("maxlon") double maxLon,
            @Field("centerlat") double centerLat, @Field("centerlon") double centerLon,
            // TODO(saemy): Add offset to the REST API.
            @Field("limit") int limit);
}
