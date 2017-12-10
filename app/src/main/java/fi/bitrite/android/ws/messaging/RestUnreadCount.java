package fi.bitrite.android.ws.messaging;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.http.HttpException;

public class RestUnreadCount extends RestClient {
    private static final String WARMSHOWERS_UNREAD_COUNT_URL = GlobalInfo.warmshowersBaseUrl + "/services/rest/message/unreadCount";

    private static final Pattern p = Pattern.compile(".*(\\d+).*");

    public RestUnreadCount(AuthenticationController authenticationController) {
        super(authenticationController);
    }

    public int getUnreadCount() throws JSONException, HttpException, IOException {
        JSONObject jsonObject = post(WARMSHOWERS_UNREAD_COUNT_URL, Collections.<NameValuePair>emptyList());
        JSONArray jsonArray = jsonObject.getJSONArray("arrayresult");
        int numMessages = jsonArray.getInt(0);
        return numMessages;
    }
}
