package fi.bitrite.android.ws.messaging;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.http.HttpException;
import org.apache.http.NameValuePair;
import org.json.JSONException;

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestUnreadCount extends RestClient {
    private static final String WARMSHOWERS_UNREAD_COUNT_URL = GlobalInfo.warmshowersBaseUrl + "/services/rest/message/unreadCount";

    private static final Pattern p = Pattern.compile(".*(\\d+).*");

    public int getUnreadCount() throws JSONException, HttpException, IOException {
        String json = getJson(WARMSHOWERS_UNREAD_COUNT_URL, Collections.<NameValuePair>emptyList());
        Matcher m = p.matcher(json);
        try {
            m.matches();
            String countStr = m.group(1);
            return Integer.valueOf(countStr);
        } catch (Exception e) {
            throw new HttpException("Error reading number of unread messages - trying to parse \"" + json + "\"");
        }
    }
}
