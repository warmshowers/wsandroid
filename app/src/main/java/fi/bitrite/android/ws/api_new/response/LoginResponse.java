package fi.bitrite.android.ws.api_new.response;

import com.google.gson.annotations.SerializedName;

import fi.bitrite.android.ws.api_new.model.ApiUser;

public class LoginResponse {
    @SerializedName("sessid") public String sessionId;
    @SerializedName("session_name") public String sessionName;
    @SerializedName("token") public String csrfToken;
    public ApiUser user;
}
