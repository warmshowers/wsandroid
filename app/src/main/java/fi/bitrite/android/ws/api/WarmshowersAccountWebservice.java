package fi.bitrite.android.ws.api;

import fi.bitrite.android.ws.api.model.ApiUser;
import fi.bitrite.android.ws.api.response.FeedbackResponse;
import fi.bitrite.android.ws.api.response.MessageThreadListResponse;
import fi.bitrite.android.ws.api.response.MessageThreadResponse;
import fi.bitrite.android.ws.api.response.PostFeedbackResponse;
import fi.bitrite.android.ws.api.response.SendMessageResponse;
import fi.bitrite.android.ws.api.response.UserSearchByKeywordResponse;
import fi.bitrite.android.ws.api.response.UserSearchByLocationResponse;
import fi.bitrite.android.ws.model.Feedback;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface WarmshowersAccountWebservice {

    @GET("services/session/token")
    Observable<Response<String>> renewCsrfToken();


    /// Users

    @GET("services/rest/user/{userId}")
    Observable<Response<ApiUser>> fetchUser(@Path("userId") int userId);

    @GET("user/{userId}/json_recommendations")
    Observable<Response<FeedbackResponse>> fetchFeedbackForRecipient(
            @Path("userId") int recipientId);

    int SEARCH_USER_DEFAULT_LIMIT = 800;

    @POST("services/rest/hosts/by_location")
    @FormUrlEncoded
    Observable<Response<UserSearchByLocationResponse>> searchUsersByLocation(
            @Field("minlat") double minLat, @Field("minlon") double minLon,
            @Field("maxlat") double maxLat, @Field("maxlon") double maxLon,
            @Field("centerlat") double centerLat, @Field("centerlon") double centerLon,
            // TODO(saemy): Add offset to the REST API.
            @Field("limit") int limit);

    @POST("services/rest/hosts/by_keyword")
    @FormUrlEncoded
    Observable<Response<UserSearchByKeywordResponse>> searchUsersByKeyword(
            @Field("keyword") String keyword, @Field("offset") int offset,
            @Field("limit") int limit);


    /// Messaging

    @POST("services/rest/message/get")
    Observable<Response<MessageThreadListResponse>> fetchMessageThreads();

    @POST("services/rest/message/getThread")
    @FormUrlEncoded
    Observable<Response<MessageThreadResponse>> fetchMessageThread(
            @Field("thread_id") int threadId);

    int MESSAGE_THREAD_STAUS_READ = 0;
    int MESSAGE_THREAD_STAUS_UNREAD = 1;

    @POST("services/rest/message/markThreadRead")
    @FormUrlEncoded
    Completable setMessageThreadReadStatus(@Field("thread_id") int threadId,
                                           @Field("status") int status);

    /**
     * Creates a new thread.
     *
     * @param recipientNames Comma separated list of usernames.
     */
    @POST("services/rest/message/send")
    @FormUrlEncoded
    Observable<Response<SendMessageResponse>> createMessageThread(
            @Field("recipients") String recipientNames, @Field("subject") String subject,
            @Field("body") String message);

    @POST("services/rest/message/reply")
    @FormUrlEncoded
    Single<Response<SendMessageResponse>> sendMessage(@Field("thread_id") int threadId,
                                                      @Field("body") String body);


    /// Feedback

    // This value must match the "minimum number of words" in the node submission settings at
    // https://www.warmshowers.org/admin/content/node-type/trust-referral
    int FEEDBACK_MIN_WORD_LENGTH = 10;

    String FEEDBACK_NODE_TYPE = "trust_referral";

    @POST("services/rest/node")
    @FormUrlEncoded
    Single<Response<PostFeedbackResponse>> giveFeedback(
            @Field("node[type]") String nodeType /* must be FEEDBACK_NODE_TYPE */,
            @Field("node[field_member_i_trust][0][uid][uid]") String recipientUsername,
            @Field("node[body]") String body,
            @Field("node[field_guest_or_host][value]") Feedback.Relation relation,
            @Field("node[field_rating][value]") Feedback.Rating rating,
            @Field("node[field_hosting_date][0][value][year]") int yearWeMet,
            @Field("node[field_hosting_date][0][value][month]") int monthWeMet);
}
