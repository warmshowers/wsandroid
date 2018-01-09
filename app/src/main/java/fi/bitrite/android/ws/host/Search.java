package fi.bitrite.android.ws.host;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.util.http.HttpException;

public interface Search {

    public List<Host> doSearch() throws JSONException, HttpException, IOException;

}
