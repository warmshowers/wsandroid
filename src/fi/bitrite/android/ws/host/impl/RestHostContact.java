package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.host.HostContact;
import fi.bitrite.android.ws.util.GlobalInfo;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends private message to a single host using the REST API.
 */
public class RestHostContact extends RestClient implements HostContact {

    private static final String WARMSHOWERS_HOST_CONTACT_URL = GlobalInfo.warmshowersBaseUrl + "/services/rest/message/send";

    @Override
    public String send(String name, String subject, String message) {
        List<NameValuePair> args = new ArrayList<NameValuePair>();
        args.add(new BasicNameValuePair("recipients", name));
        args.add(new BasicNameValuePair("subject", subject));
        args.add(new BasicNameValuePair("body", message));
        String json = getJson(WARMSHOWERS_HOST_CONTACT_URL, args);
        return json;
    }

}
