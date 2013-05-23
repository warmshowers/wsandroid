package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.api.HttpReader;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.model.HostBriefInfo;

import java.util.List;

public class HttpTextSearch extends HttpReader implements Search {

    private final String text;

    public HttpTextSearch(String text) {
        this.text = text;
    }

    /*
     * Scrapes the standard WarmShowers list search page.
     */
    public List<HostBriefInfo> doSearch() {
        String simpleUrl = "https://www.warmshowers.org/search/wsuser/" + text;
        String html = getPage(simpleUrl);
        HttpTextSearchResultScraper scraper = new HttpTextSearchResultScraper(html);
        List<HostBriefInfo> hosts = scraper.getHosts();
        return hosts;
    }
}