package fi.bitrite.android.ws.search.impl;

import java.util.Arrays;
import java.util.List;

import android.util.Log;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.search.Search;

public class MockTextSearch implements Search {

	private String text;

	public MockTextSearch(String text) {
		this.text = text;
	}

	public List<HostBriefInfo> doSearch() {
		Log.d("doSearch", "Text search using argument: ''" + text + "''");
		HostBriefInfo[] hosts = { new HostBriefInfo("jstaffans", "Johannes Staffans", "Helsinki", "Comments"),
				new HostBriefInfo("adambad", "Adam Bad", "Helsinki", "Comments") };
		return Arrays.asList(hosts);
	}

}
