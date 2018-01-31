package fi.bitrite.android.ws.model;

import java.util.Date;

public class Message {
    public final static int STATUS_SYNCED = 0;
    public final static int STATUS_OUTGOING = 1; // In the outbox but not yet pushed to the server.


    public final int id;

    public final int threadId;
    public final int authorId;

    public final Date date;
    public final String body;

    public int status;

    public Message(int id, int threadId, int authorId, Date date, String body, int status) {
        this.id = id;
        this.threadId = threadId;
        this.authorId = authorId;
        this.date = date;
        this.body = body;
        this.status = status;
    }
}