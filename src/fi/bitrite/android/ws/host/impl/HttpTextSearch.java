package fi.bitrite.android.ws.host.impl;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpException;

import java.util.ArrayList;
import java.util.List;

public class HttpTextSearch extends RestClient implements Search {

    private final String keyword;

    public HttpTextSearch(String keyword) {
        this.keyword = keyword;
    }
    private static final String WARMSHOWERS_HOST_BY_KEYWORD_URL = "https://www.warmshowers.org/services/rest/hosts/by_keyword";

    /*
     * Searches wsuser using service.
     *
     * The whole concept of HostBriefInfo here comes from previously
     * scraping from an HTML page, but to make changes incremental
     * it is adapted here to the new host/by_keyword service. Eventually
     * we should just use the info returned by that service and move on.
     *
     */
    public List<HostBriefInfo> doSearch() {

        List<NameValuePair> args = new ArrayList<NameValuePair>();
        args.add(new BasicNameValuePair("keyword", this.keyword));
        post(WARMSHOWERS_HOST_BY_KEYWORD_URL, args);

        try {
            String json = getJson(WARMSHOWERS_HOST_BY_KEYWORD_URL, args);
            JSONObject hostJson = new JSONObject(json).getJSONObject("accounts");
            List<HostBriefInfo> list = new ArrayList<HostBriefInfo>();

            for (int i = 0; i < hostJson.names().length(); i++) {
                int uid = hostJson.names().getInt(i);
                JSONObject account = (JSONObject) (hostJson.get(Integer.toString(uid)));
                String username = (String) account.get("name");
                String fullname = (String) account.get("fullname");
                String location = account.get("city") + ", " + account.get("province");
                String comments = (String) account.get("comments");
                HostBriefInfo bi = new HostBriefInfo(uid, username, fullname, location, comments);
                list.add(bi);
            }
            return list;
        } catch (JSONException e) {
            throw new HttpException(e);
        }
    }
}