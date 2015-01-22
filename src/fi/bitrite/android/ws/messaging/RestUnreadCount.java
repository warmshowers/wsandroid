package fi.bitrite.android.ws.messaging;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.http.HttpException;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestUnreadCount extends RestClient {
    private static final String WARMSHOWERS_UNREAD_COUNT_URL = GlobalInfo.warmshowersBaseUrl + "/services/rest/message/unreadCount";

    private static final Pattern p = Pattern.compile(".*(\\d+).*");

    public int getUnreadCount() throws JSONException, HttpException, IOException, RestClientRecursionException {
        JSONObject jsonObject = post(WARMSHOWERS_UNREAD_COUNT_URL, Collections.<NameValuePair>emptyList(), 1);
        JSONArray jsonArray = jsonObject.getJSONArray("arrayresult");
        int numMessages = jsonArray.getInt(0);
        return numMessages;
    }
}
