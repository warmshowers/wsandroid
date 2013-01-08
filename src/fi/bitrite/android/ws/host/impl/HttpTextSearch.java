package fi.bitrite.android.ws.host.impl;

import java.util.List;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.model.HostBriefInfo;

public class HttpTextSearch extends HttpReader implements Search {

	private String text;

	public HttpTextSearch(String text, HttpAuthenticationService authenticationService,
			HttpSessionContainer sessionContainer) {
		super(authenticationService, sessionContainer);
		this.text = text;
	}

	/*
	 * Scrapes the standard WarmShowers list search page.
	 */
	public List<HostBriefInfo> doSearch() {
		String simpleUrl = "http://www.warmshowers.org/search/wsuser/" + text;
		String html = getPage(simpleUrl);
		HttpTextSearchResultScraper scraper = new HttpTextSearchResultScraper(html);
		List<HostBriefInfo> hosts = scraper.getHosts();
		return hosts;
	}
}