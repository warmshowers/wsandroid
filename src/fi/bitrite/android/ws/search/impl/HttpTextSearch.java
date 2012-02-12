package fi.bitrite.android.ws.search.impl;

import java.util.ArrayList;
import java.util.List;

import fi.bitrite.android.ws.auth.AuthenticationService;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.search.Search;

public class HttpTextSearch implements Search {

	AuthenticationService authenticationService;
	
	String text;
	
	public HttpTextSearch(String text, AuthenticationService authenticationService) {
		this.text = text;
		this.authenticationService = authenticationService;
	}

	/*
	 * Scrapes the standard WarmShowers list search page.
	 */
	public List<Host> doSearch() {
		authenticateUserIfNeeded();
		
		return new ArrayList<Host>();
	}

	private void authenticateUserIfNeeded() {
		if (!authenticationService.isAuthenticated()) {
			authenticationService.authenticate();
		}
	}

}
