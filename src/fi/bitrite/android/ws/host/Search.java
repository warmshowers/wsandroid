package fi.bitrite.android.ws.host;

import org.json.JSONException;

import java.util.List;

import fi.bitrite.android.ws.activity.model.HostInformation;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpException;

public interface Search {
    
    public List<HostBriefInfo> doSearch() throws HttpException, JSONException;

}
