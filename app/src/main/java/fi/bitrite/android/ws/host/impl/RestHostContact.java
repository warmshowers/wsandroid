package fi.bitrite.android.ws.host.impl;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.http.HttpException;

/**
 * Sends private message to a single host using the REST API.
 */
public class RestHostContact extends RestClient {

    private static final String WARMSHOWERS_HOST_CONTACT_URL = GlobalInfo.warmshowersBaseUrl + "/services/rest/message/send";

    public RestHostContact(AuthenticationController authenticationController) {
        super(authenticationController);
    }

    public JSONObject send(String name, String subject, String message) throws JSONException, HttpException, IOException {
        List<NameValuePair> args = new ArrayList<NameValuePair>();
        args.add(new BasicNameValuePair("recipients", name));
        args.add(new BasicNameValuePair("subject", subject));
        args.add(new BasicNameValuePair("body", message));
        JSONObject jsonObject = post(WARMSHOWERS_HOST_CONTACT_URL, args);
        return jsonObject;
    }
}
