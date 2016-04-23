package fi.bitrite.android.ws.host;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpException;

public interface Search {

    public List<HostBriefInfo> doSearch() throws JSONException, HttpException, IOException;

}
