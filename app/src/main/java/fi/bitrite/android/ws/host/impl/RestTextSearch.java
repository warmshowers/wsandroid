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
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.http.HttpException;

public class RestTextSearch extends RestClient implements Search {

    private final String keyword;

    public RestTextSearch(AuthenticationController authenticationController, String keyword) {
        super(authenticationController);

        this.keyword = keyword;
    }

    private static final String WARMSHOWERS_HOST_BY_KEYWORD_URL = GlobalInfo.warmshowersBaseUrl + "/services/rest/hosts/by_keyword";

    /*
     * Searches wsuser using service.
     *
     * The whole concept of HostBriefInfo here comes from previously
     * scraping from an HTML page, but to make changes incremental
     * it is adapted here to the new host/by_keyword service. Eventually
     * we should just use the info returned by that service and move on.
     *
     */
    public List<Host> doSearch() throws JSONException, HttpException, IOException {

        List<NameValuePair> args = new ArrayList<NameValuePair>();
        args.add(new BasicNameValuePair("keyword", this.keyword));

        JSONObject jsonObject = post(WARMSHOWERS_HOST_BY_KEYWORD_URL, args);

        List<Host> list = new ArrayList<>();
        JSONObject statusJson = jsonObject.getJSONObject("status");
        String numDelivered = statusJson.get("delivered").toString();
        if (Integer.parseInt(numDelivered) > 0) {
            JSONObject hostJson = jsonObject.getJSONObject("accounts");

            for (int i = 0; i < hostJson.names().length(); i++) {
                int uid = hostJson.names().getInt(i);
                JSONObject account = (JSONObject) (hostJson.get(Integer.toString(uid)));
                Host user = Host.CREATOR.parse(account);
                list.add(user);
            }
        }
        return list;
    }
}
