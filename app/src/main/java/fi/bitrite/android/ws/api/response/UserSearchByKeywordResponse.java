package fi.bitrite.android.ws.api.response;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

import fi.bitrite.android.ws.api.model.ApiUser;

public class UserSearchByKeywordResponse {
    // {
    //    "status": {"delivered":30,"status":"complete"},
    //    "query_data":{
    //      "sql": {}
    //      "keyword":"the-fastest-cyclist",
    //      "offset":0,
    //      "limit":50
    //    },
    //    "accounts":[ userId1: { user1 }, userId2: { user2 }, ... ]
    // }


    public static class Status {
        @SerializedName("delivered") public int numDelivered;
        @SerializedName("status") public String status; // Is there anything else than "complete"?
    }

    @SerializedName("status") public Status status;
    // @SerializedName("query_data") public QueryData queryData; // We are not interested in this.
    @SerializedName("accounts") public Map<Integer, ApiUser> users;
}
