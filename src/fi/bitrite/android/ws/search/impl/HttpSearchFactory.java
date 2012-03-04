package fi.bitrite.android.ws.search.impl;

import com.google.android.maps.GeoPoint;
import com.google.inject.Inject;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.search.Search;
import fi.bitrite.android.ws.search.SearchFactory;

public class HttpSearchFactory implements SearchFactory {

	@Inject
	HttpAuthenticationService authenticationService;

	@Inject
	HttpSessionContainer sessionContainer;

	public Search createTextSearch(String text) {
		return new HttpTextSearch(text, authenticationService, sessionContainer);
	}

	public Search createMapSearch(GeoPoint topLeft, GeoPoint bottomRight, int numHostsCutoff) {
		return new HttpMapSearch(topLeft, bottomRight, numHostsCutoff, sessionContainer);
	}

}
