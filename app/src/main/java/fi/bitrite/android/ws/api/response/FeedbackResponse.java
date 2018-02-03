package fi.bitrite.android.ws.api.response;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import fi.bitrite.android.ws.api.model.ApiFeedback;

@JsonAdapter(FeedbackResponse.Deserializer.class)
public class FeedbackResponse {
    public List<ApiFeedback> feedbacks = new ArrayList<>();


    /**
     * This deserializer removes the undesired indirection which would require an intermediate type:
     *     { "recommendations": [ {"recommendation": {xxx}}, {"recommendation": {yyy}} ] }
     * becomes
     *     { "recommendations": [ {xxx}, {yyy} ] }
     */
    public static class Deserializer implements JsonDeserializer<FeedbackResponse> {

        @Override
        public FeedbackResponse deserialize(
                JsonElement jsonElement, Type type,
                JsonDeserializationContext context) throws JsonParseException {

            FeedbackResponse result = new FeedbackResponse();

            final JsonObject root = jsonElement.getAsJsonObject();
            final JsonArray jsonRecommendations = root.getAsJsonArray("recommendations");
            for (JsonElement intermediate : jsonRecommendations) {
                final JsonElement jsonFeedback =
                        intermediate.getAsJsonObject().get("recommendation");
                final ApiFeedback feedback =
                        context.deserialize(jsonFeedback, ApiFeedback.class);

                result.feedbacks.add(feedback);
            }

            return result;
        }
    }
}

