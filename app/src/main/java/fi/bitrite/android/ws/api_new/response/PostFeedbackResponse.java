package fi.bitrite.android.ws.api_new.response;

import com.google.gson.annotations.SerializedName;

public class PostFeedbackResponse {
    // {
    //      "nid":"21332",
    //      "uri":"https://www.warmshowers.org/services/rest/node/21332"
    // }

    @SerializedName("nid") public int feedbackId;
    public String uri;
}
