package fi.bitrite.android.ws.api.typeadapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;

/**
 * Booleans are returned as "0" and "1" instead of 0 and 1.
 */
public class BooleanDeserializer implements JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonElement jsonElement, Type type,
                               JsonDeserializationContext jsonDeserializationContext) {
        return jsonElement.getAsString().equals("1");
    }
}
