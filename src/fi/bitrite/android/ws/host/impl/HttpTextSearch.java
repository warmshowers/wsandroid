package fi.bitrite.android.ws.host.impl;

import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.http.HttpException;

import java.util.ArrayList;
import java.util.List;

public class HttpTextSearch extends RestClient implements Search {

    private final String keyword;

    public HttpTextSearch(String keyword) {
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
    public List<HostBriefInfo> doSearch() throws JSONException, HttpException {

        List<NameValuePair> args = new ArrayList<NameValuePair>();
        args.add(new BasicNameValuePair("keyword", this.keyword));
        post(WARMSHOWERS_HOST_BY_KEYWORD_URL, args);

        String json = getJson(WARMSHOWERS_HOST_BY_KEYWORD_URL, args);
        JSONObject allJson = new JSONObject(json);

        List<HostBriefInfo> list = new ArrayList<HostBriefInfo>();
        JSONObject statusJson = allJson.getJSONObject("status");
        String numDelivered = statusJson.get("delivered").toString();
        if (Integer.parseInt(numDelivered) > 0) {
            JSONObject hostJson = allJson.getJSONObject("accounts");

            for (int i = 0; i < hostJson.names().length(); i++) {
                int uid = hostJson.names().getInt(i);
                JSONObject account = (JSONObject) (hostJson.get(Integer.toString(uid)));
                HostBriefInfo bi = new HostBriefInfo(
                        uid,
                        account.get("name").toString(),
                        account.get("fullname").toString(),
                        account.get("street").toString(),
                        account.get("city").toString(),
                        account.get("province").toString(),
                        account.get("country").toString(),
                        account.get("comments").toString(),
                        (account.get("notcurrentlyavailable").toString().equals("1"))
                );
                list.add(bi);
            }
        }
        return list;
    }
}