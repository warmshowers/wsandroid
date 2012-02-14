package fi.bitrite.android.ws.search.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.util.Log;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.search.Search;

public class HttpTextSearch implements Search {

	private static final String WARMSHOWERS_LIST_SEARCH_URL = "http://www.warmshowers.org/search/wsuser/";

	HttpAuthenticationService authenticationService;

	HttpSessionContainer sessionContainer;

	String text;

	public HttpTextSearch(String text, HttpAuthenticationService authenticationService,
			HttpSessionContainer sessionContainer) {
		this.text = text;
		this.authenticationService = authenticationService;
		this.sessionContainer = sessionContainer;
	}

	/*
	 * Scrapes the standard WarmShowers list search page.
	 */
	public List<Host> doSearch() {
		authenticateUserIfNeeded();
		String html = getSearchResultHtml();
		List<Host> hosts = scrapeHostDetails(html);
		return hosts;
	}

	protected void authenticateUserIfNeeded() {
		if (!authenticationService.isAuthenticated()) {
			authenticationService.authenticate();
		}
	}

	protected String getSearchResultHtml() {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(WARMSHOWERS_LIST_SEARCH_URL + text);
			HttpContext context = sessionContainer.getSessionContext();

			HttpResponse response = client.execute(get, context);
			HttpEntity entity = response.getEntity();

			Log.d("getSearchResultHtml", response.getStatusLine().toString());

			EntityUtils.toString(entity);
		}

		catch (Exception e) {
			throw new SearchFailedException(e);
		}

		return null;
	}

	protected List<Host> scrapeHostDetails(String html) {
		return new ArrayList<Host>();
	}

}
