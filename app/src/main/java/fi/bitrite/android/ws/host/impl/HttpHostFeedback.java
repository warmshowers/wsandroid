package fi.bitrite.android.ws.host.impl;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.util.GlobalInfo;

/**
 * Retrieves feedback for a given host.
 */
public class HttpHostFeedback extends RestClient {

    public HttpHostFeedback(AuthenticationController authenticationController) {
        super(authenticationController);
    }

    /**
     * Given the ID of a WarmShowers user, retrieve feedback about him.
     *
     * @param id
     * @return ArrayList
     */
    public ArrayList<Feedback> getFeedback(int id) throws JSONException, URISyntaxException, IOException {
        String simpleUrl = new StringBuilder().append(GlobalInfo.warmshowersBaseUrl).append("/user/")
                .append(id).append("/json_recommendations").toString();
        JSONObject jsonObject = get(simpleUrl);

        FeedbackJsonParser parser = new FeedbackJsonParser(jsonObject);
        return parser.getFeedback();
    }
}
