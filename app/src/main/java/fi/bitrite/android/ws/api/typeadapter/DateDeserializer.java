package fi.bitrite.android.ws.api.typeadapter;


import android.annotation.SuppressLint;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The default GSON date deserializer expects dates to be given in milliseconds since the epoch.
 * However, the REST API provides dates in seconds since the epoch.
 * And sometimes it is returned as a date string.
 */
public class DateDeserializer implements JsonDeserializer<Date> {
    @SuppressLint("SimpleDateFormat")
    private final static SimpleDateFormat stringDateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public Date deserialize(JsonElement jsonElement, Type type,
                            JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        final JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();

        // Integer version.
        try {
            final long dateInS = jsonPrimitive.getAsLong();
            return new Date(dateInS * 1000);
        } catch (NumberFormatException e) {
            // Ignore.
        }

        // String version.
        try {
            final String value = jsonPrimitive.getAsString();
            if (value.isEmpty()) {
                return null;
            }

            return stringDateFormat.parse(value);
        } catch (ParseException e) {
            throw new JsonParseException("Invalid date format", e);
        }
    }
}
