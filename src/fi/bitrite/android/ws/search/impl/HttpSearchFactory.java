package fi.bitrite.android.ws.search.impl;

import com.google.inject.Inject;

import fi.bitrite.android.ws.auth.AuthenticationService;
import fi.bitrite.android.ws.search.Search;
import fi.bitrite.android.ws.search.SearchFactory;

public class HttpSearchFactory implements SearchFactory {

	@Inject AuthenticationService authenticationService;
	
	public Search createTextSearch(String text) {
		return new HttpTextSearch(text, authenticationService);
	}

}
 