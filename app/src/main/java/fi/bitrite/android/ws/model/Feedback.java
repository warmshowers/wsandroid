package fi.bitrite.android.ws.model;

import java.util.Date;

public class Feedback {
    public enum Relation {
        Guest,
        Host,
        MetWhileTraveling,
        Other
    }

    public enum Rating {
        Positive,
        Neutral,
        Negative
    }


    public final int id;
    public final int recipientId;
    public final int senderId;
    public final String senderFullname;

    public final Relation relation;
    public final Rating rating;
    public final Date meetingDate;

    public final String body;


    public Feedback(int id, int recipientId, int senderId, String senderFullname, Relation relation,
                    Rating rating, Date meetingDate, String body) {
        this.id = id;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.senderFullname = senderFullname;
        this.relation = relation;
        this.rating = rating;
        this.meetingDate = meetingDate;
        this.body = body;
    }
}
