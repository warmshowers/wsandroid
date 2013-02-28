package fi.bitrite.android.ws.host.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.util.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a JSON string containing zero or more recommendations for a host.
 */
public class FeedbackJsonParser {

    private final String json;

    public FeedbackJsonParser(String json) {
        this.json = json;
    }

    public ArrayList<Feedback> getFeedback() {
        ArrayList<Feedback> feedback = new ArrayList<Feedback>();

        try {
            JSONObject jsonObj = new JSONObject(this.json);
            JSONArray recommendations = jsonObj.getJSONArray("recommendations");
            for (int i = 0; i < recommendations.length(); i++) {
                JSONObject recommendation = recommendations.getJSONObject(i);
                feedback.add(Feedback.CREATOR.parse(recommendation.getJSONObject("recommendation")));
            }
        }

        catch (Exception e) {
            throw new HttpException(e);
        }

        return feedback;
    }

}
