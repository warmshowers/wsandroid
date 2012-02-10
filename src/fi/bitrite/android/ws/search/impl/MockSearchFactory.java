package fi.bitrite.android.ws.search.impl;

import fi.bitrite.android.ws.search.Search;
import fi.bitrite.android.ws.search.SearchFactory;

public class MockSearchFactory implements SearchFactory {

	public Search createTextSearch(String text) {
		return new MockTextSearch(text);
	}

}
