package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.util.GlobalInfo;

import java.util.ArrayList;

/**
 * Retrieves feedback for a given host.
 */
public class HttpHostFeedback extends RestClient {

    /**
     * Given the ID of a WarmShowers user, retrieve feedback about him.
     *
     * @param id
     * @return ArrayList
     */
    public ArrayList<Feedback> getFeedback(int id) {
        String simpleUrl = new StringBuilder().append(GlobalInfo.warmshowersBaseUrl).append("/user/")
                .append(id).append("/json_recommendations").toString();
        String json = get(simpleUrl);

        FeedbackJsonParser parser = new FeedbackJsonParser(json);
        return parser.getFeedback();
    }
}
