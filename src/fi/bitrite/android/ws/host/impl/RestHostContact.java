package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.http.HttpException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends private message to a single host using the REST API.
 */
// TODO: There's no reason for this to be a subclass
public class RestHostContact extends RestClient {

    private static final String WARMSHOWERS_HOST_CONTACT_URL = GlobalInfo.warmshowersBaseUrl + "/services/rest/message/send";

    public String send(String name, String subject, String message) throws JSONException, HttpException, IOException {
        List<NameValuePair> args = new ArrayList<NameValuePair>();
        args.add(new BasicNameValuePair("recipients", name));
        args.add(new BasicNameValuePair("subject", subject));
        args.add(new BasicNameValuePair("body", message));
        String json = post(WARMSHOWERS_HOST_CONTACT_URL, args);
        return json;
    }

}
