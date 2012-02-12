package fi.bitrite.android.ws.search.impl;

import fi.bitrite.android.ws.search.Search;
import fi.bitrite.android.ws.search.SearchFactory;

public class HttpSearchFactory implements SearchFactory {

	public Search createTextSearch(String text) {
		return new HttpTextSearch(text);
	}

}
 