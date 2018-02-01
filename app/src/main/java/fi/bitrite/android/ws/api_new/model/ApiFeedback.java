package fi.bitrite.android.ws.api_new.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.Date;

import fi.bitrite.android.ws.model.Feedback;

public class ApiFeedback {
    // {
    //   "nid":"158612",
    //   "fullname":"Milan Egloff and Daniela Epple",
    //   "name":"Kinglui01",
    //   "uid":"71604",
    //   "body":"We had a very pleasant stay with J\u00f6rg. He shows us the city around ...",
    //   "field_hosting_date":1467324000,
    //   "field_guest_or_host":"Host",
    //   "name_1":"j.o.r.g",
    //   "uid_1":"29498",
    //   "field_rating":"Positive",
    //   "field_hosting_date":"2017-02-11T13:33:00",
    //   "field_guest_or_host":"Host",
    //   "field_rating":"Positive"
    // }

    @SerializedName("nid") public int id;
    @SerializedName("field_hosting_date") public Date meetingDate;
    @SerializedName("field_guest_or_host") @JsonAdapter(RelationDeserializer.class) public Feedback.Relation relation;
    @SerializedName("field_rating") @JsonAdapter(RatingDeserializer.class) public Feedback.Rating rating;

    @SerializedName("uid_1") public int recipientId;
    @SerializedName("name_1") public String recipientName;

    @SerializedName("uid") public int senderId;
    @SerializedName("name") public String senderName;
    @SerializedName("fullname") public String senderFullname;

    public String body;


    public Feedback toFeedback() {
        return new Feedback(
                id, recipientId, senderId, senderFullname, relation, rating, meetingDate, body);
    }


    public class RelationDeserializer implements JsonDeserializer<Feedback.Relation> {
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
    }

    public class RatingDeserializer implements JsonDeserializer<Feedback.Rating> {
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
    }
}
