package fi.bitrite.android.ws.host.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.util.http.HttpException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * User: johannes
 * Date: 25.02.2013
 */
public class FeedbackJsonParser {

    private final String json;

    public FeedbackJsonParser(String json) {
        this.json = json;
    }

    public List<Feedback> getFeedback() {
        List<Feedback> feedback = new ArrayList<Feedback>();

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObj = jsonParser.parse(json).getAsJsonObject();
        JsonArray recommendations = jsonObj.getAsJsonArray("recommendations");
        for (JsonElement element : recommendations) {
            JsonObject recommendation = element.getAsJsonObject().get("recommendation").getAsJsonObject();
            System.out.println(recommendation.get("fullname"));
            feedback.add(new Feedback());
        }

        return feedback;
    }

}
