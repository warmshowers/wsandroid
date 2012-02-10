package fi.bitrite.android.ws.persistence.impl;

import java.util.Arrays;
import java.util.List;

import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.TestHostFactory;
import fi.bitrite.android.ws.persistence.StarredHostDao;

public class StarredHostDaoImpl implements StarredHostDao {

	private Host testHost = TestHostFactory.getHostFromJson();
	
	public List<Host> getAll() {
		Host [] starredHosts = {  testHost, testHost };
		return Arrays.asList(starredHosts);
	}

	public Host get() {
		return testHost;
	}
	
}
