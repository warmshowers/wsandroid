package fi.bitrite.android.ws.persistence.impl;

import java.util.Arrays;
import java.util.List;

import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.model.TestHostFactory;
import fi.bitrite.android.ws.persistence.StarredHostDao;

public class StarredHostDaoImpl implements StarredHostDao {

	private Host testHost = TestHostFactory.getHostFromJson();
	
	public List<HostBriefInfo> getAllBrief() {
		HostBriefInfo [] starredHosts = {  new HostBriefInfo(123, testHost), new HostBriefInfo(123, testHost) };
		return Arrays.asList(starredHosts);
	}

	public Host get() {
		return testHost;
	}

	public boolean isHostStarred(int id, String name) {
		return false;
	}
	
}
