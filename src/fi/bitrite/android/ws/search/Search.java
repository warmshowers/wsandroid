package fi.bitrite.android.ws.search;

import java.util.List;

import fi.bitrite.android.ws.model.HostBriefInfo;

public interface Search {
	
	public List<HostBriefInfo> doSearch();

}
