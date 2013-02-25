package fi.bitrite.android.ws.host.impl;

import com.google.android.maps.GeoPoint;
import com.google.inject.Inject;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.SearchFactory;

public class WsSearchFactory implements SearchFactory {

	@Inject
	HttpAuthenticationService authenticationService;

	@Inject
	HttpSessionContainer sessionContainer;

	public Search createTextSearch(String text) {
		return new HttpTextSearch(text, authenticationService, sessionContainer);
	}

	public Search createMapSearch(GeoPoint topLeft, GeoPoint bottomRight, int numHostsCutoff) {
		return new RestMapSearch(topLeft, bottomRight, numHostsCutoff, authenticationService, sessionContainer);
	}

}
