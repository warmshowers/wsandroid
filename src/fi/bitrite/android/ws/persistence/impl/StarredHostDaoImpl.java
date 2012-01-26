package fi.bitrite.android.ws.persistence.impl;

import java.util.Arrays;
import java.util.List;

import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;

public class StarredHostDaoImpl implements StarredHostDao {

	public List<Host> getStarredHosts() {
		Host [] starredHosts = {  new Host("John Smith", "I'm a nice guy"),  new Host("Bob Dole", "Evil host") };
		return Arrays.asList(starredHosts);
	}

	public Host getStarredHost() {
		return new Host("John Smith", "I'm a nice guy");
	}
	
	
}
