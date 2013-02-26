package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.model.Feedback;

import java.util.List;

/**
 * Retrieves feedback for a given host.
 */
public class HttpHostFeedback extends HttpReader {

    public HttpHostFeedback(HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer) {
        super(authenticationService, sessionContainer);
    }

    /**
     * Given the ID of a WarmShowers user, retrieve feedback about him.
     * @param id
     * @return List
     */
    public List<Feedback> getFeedback(int id) {
        String simpleUrl = new StringBuilder().append("http://www.warmshowers.org/user/")
                .append(id).append("/json_recommendations").toString();
        String json = getPage(simpleUrl);

        FeedbackJsonParser parser = new FeedbackJsonParser(json);
        return parser.getFeedback();
    }
}
