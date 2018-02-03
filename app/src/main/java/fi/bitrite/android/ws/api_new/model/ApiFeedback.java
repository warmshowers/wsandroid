package fi.bitrite.android.ws.api_new.model;

import com.google.gson.annotations.SerializedName;

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
    @SerializedName("field_guest_or_host") public Feedback.Relation relation;
    @SerializedName("field_rating") public Feedback.Rating rating;

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
}
