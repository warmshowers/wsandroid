package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.util.http.HttpException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Parses a JSON string containing zero or more recommendations for a host.
 */
public class FeedbackJsonParser {

    private final JSONObject mJSONObj;

    public FeedbackJsonParser(JSONObject json) {
        mJSONObj = json;
    }

    public ArrayList<Feedback> getFeedback() {
        ArrayList<Feedback> feedback = new ArrayList<Feedback>();

        try {
            JSONArray recommendations = mJSONObj.getJSONArray("recommendations");
            for (int i = 0; i < recommendations.length(); i++) {
                JSONObject recommendation = recommendations.getJSONObject(i);
                feedback.add(Feedback.CREATOR.parse(recommendation.getJSONObject("recommendation")));
            }
        } catch (Exception e) {
            throw new HttpException(e);
        }

        return feedback;
    }

}
