package fi.bitrite.android.ws.api.typeadapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import fi.bitrite.android.ws.model.Feedback;

public class RelationTypeAdapter implements JsonDeserializer<Feedback.Relation> {

    @Override
    public Feedback.Relation deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        final String value = json.getAsString();

        switch (value) {
            case "Guest": return Feedback.Relation.Guest;
            case "Host": return Feedback.Relation.Host;
            case "Met Traveling": // Fall-through
            case "Met_Traveling": return Feedback.Relation.MetWhileTraveling;
            case "Other": return Feedback.Relation.Other;

            default:
                throw new JsonParseException("Unknown relation type: " + value);
        }
    }

    public static String serialize(Feedback.Relation relation) {
        switch (relation) {
            case Guest: return "Guest";
            case Host: return "Host";
            case MetWhileTraveling: return "Met Traveling";
            case Other: return "Other";

            default:
                throw new RuntimeException("Unknown relation.");
        }
    }
}

