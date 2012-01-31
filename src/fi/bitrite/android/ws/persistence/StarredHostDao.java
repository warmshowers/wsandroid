package fi.bitrite.android.ws.persistence;

import java.util.List;

import fi.bitrite.android.ws.model.Host;

public interface StarredHostDao {

	public List<Host> getAll();

	public Host get();
	
}
