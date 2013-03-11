package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.api.HttpReader;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;

/**
 * User: johannes
 * Date: 24.02.2013
 */
public class HttpHostId extends HttpReader {

    private final String name;

    public HttpHostId(String name, HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer) {
        super(authenticationService, sessionContainer);
        this.name = name;
    }

    public int getHostId(String hostName) {
        String simpleUrl = new StringBuilder("http://www.warmshowers.org/users/").append(hostName).toString();
        String html = getPage(simpleUrl);
        HttpHostIdScraper scraper = new HttpHostIdScraper(html);
        return scraper.getId();
    }
}
