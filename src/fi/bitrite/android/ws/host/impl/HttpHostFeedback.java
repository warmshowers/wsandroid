package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.api.HttpReader;
import fi.bitrite.android.ws.model.Feedback;

import java.util.ArrayList;

/**
 * Retrieves feedback for a given host.
 */
public class HttpHostFeedback extends HttpReader {

    /**
     * Given the ID of a WarmShowers user, retrieve feedback about him.
     * @param id
     * @return ArrayList
     */
    public ArrayList<Feedback> getFeedback(int id) {
        String simpleUrl = new StringBuilder().append("https://www.warmshowers.org/user/")
                .append(id).append("/json_recommendations").toString();
        String json = getPage(simpleUrl);

        FeedbackJsonParser parser = new FeedbackJsonParser(json);
        return parser.getFeedback();
    }
}
