package fi.bitrite.android.ws.persistence;

import java.util.List;

import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;

public interface StarredHostDao {

	public Host get();

	public List<HostBriefInfo> getAllBrief();
	
}
