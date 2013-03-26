package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.api.HttpReader;

/**
 * User: johannes
 */
public class HttpHostId extends HttpReader {

    private final String name;

    public HttpHostId(String name) {
        this.name = name;
    }

    public int getHostId(String hostName) {
        String simpleUrl = new StringBuilder("http://www.warmshowers.org/users/").append(hostName).toString();
        String html = getPage(simpleUrl);
        HttpHostIdScraper scraper = new HttpHostIdScraper(html);
        return scraper.getId();
    }
}
