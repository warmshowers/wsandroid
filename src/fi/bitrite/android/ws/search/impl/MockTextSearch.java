package fi.bitrite.android.ws.search.impl;

import java.util.Arrays;
import java.util.List;

import android.util.Log;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.TestHostFactory;
import fi.bitrite.android.ws.search.Search;

public class MockTextSearch implements Search {

	private String text;
	
	private Host testHost = TestHostFactory.getHostFromJson(); 

	public MockTextSearch(String text) {
		this.text = text;
	}

	public List<Host> doSearch() {
		Log.d("doSearch", "Text search using argument: ''" + text + "''");
		Host [] starredHosts = {  testHost, testHost };
		return Arrays.asList(starredHosts);
	}

}
 