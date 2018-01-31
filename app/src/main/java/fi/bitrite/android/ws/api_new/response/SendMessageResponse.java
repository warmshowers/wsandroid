package fi.bitrite.android.ws.api_new.response;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(SendMessageResponse.Deserializer.class)
public class SendMessageResponse {
    // Successful result: [ true ]

    public boolean isSuccessful;

    /**
     * This deserializer interprets the boolean array into a single boolean.
     *     [ true ]
     * becomes
     *     successful: true
     */
    public static class Deserializer implements JsonDeserializer<SendMessageResponse> {

        @Override
        public SendMessageResponse deserialize(
                JsonElement jsonElement, Type type,
                JsonDeserializationContext context) throws JsonParseException {

            SendMessageResponse result = new SendMessageResponse();

            final JsonArray root = jsonElement.getAsJsonArray();
            result.isSuccessful = root.getAsBoolean();

            return result;
        }
    }
}
