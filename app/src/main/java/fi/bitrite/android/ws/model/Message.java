package fi.bitrite.android.ws.model;

import java.util.Date;

public class Message {

    public final int id;

    public final int threadId;
    public final int authorId;

    public final Date date;
    public final String body;

    /**
     * Whether this message is new and a notification should be shown for it.
     */
    public final boolean isNew;

    /**
     * Whether this message is already pushed to the webservice. We need this flag for a very short
     * time between a message being successfully sent and the thread being reloaded from the
     * webservice. As we do not get any id for the newly created message, we cannot map the one from
     * the remote to the local one. And since it is possible that one message was successfully sent
     * and another not we need to keep track of that information. When the next reload is done from
     * the webservice, all the temporary local messages which are flagged as pushed are removed.
     * TODO(saemy): Change the webservice to return the message(id) upon successful push.
     */
    public final boolean isPushed;

    public Message(int id, int threadId, int authorId, Date date, String body, boolean isNew,
                   boolean isPushed) {
        this.id = id;
        this.threadId = threadId;
        this.authorId = authorId;
        this.date = date;
        this.body = body;
        this.isNew = isNew;
        this.isPushed = isPushed;
    }

    public Message cloneForIsNew(boolean isNew) {
        return new Message(id, threadId, authorId, date, body, isNew, isPushed);
    }
    public Message cloneForIsPushed(boolean isPushed) {
        return new Message(id, threadId, authorId, date, body, isNew, isPushed);
    }
}
