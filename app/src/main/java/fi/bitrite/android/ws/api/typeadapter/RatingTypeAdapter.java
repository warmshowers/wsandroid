package fi.bitrite.android.ws.api.typeadapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import fi.bitrite.android.ws.model.Feedback;

public class RatingTypeAdapter implements JsonDeserializer<Feedback.Rating> {

    @Override
    public Feedback.Rating deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        final String value = json.getAsString();

        switch (value) {
            case "Positive": return Feedback.Rating.Positive;
            case "Neutral": return Feedback.Rating.Neutral;
            case "Negative": return Feedback.Rating.Negative;

            default:
                throw new JsonParseException("Unknown rating type: " + value);
        }
    }

    public static String serialize(Feedback.Rating rating) {
        switch (rating) {
            case Positive: return "Positive";
            case Neutral: return "Neutral";
            case Negative: return "Negative";

            default:
                throw new RuntimeException("Unknown rating.");
        }
    }
}
