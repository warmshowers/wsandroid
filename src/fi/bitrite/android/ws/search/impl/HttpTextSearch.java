package fi.bitrite.android.ws.search.impl;

import java.util.List;

import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.search.Search;

public class HttpTextSearch implements Search {

	private String text;
	
	public HttpTextSearch(String text) {
		this.text = text;
	}

	public List<Host> doSearch() {
		// TODO Auto-generated method stub
		return null;
	}

}
