package fi.bitrite.android.ws.model;

import android.text.Html;
import android.text.Spanned;

import java.util.Date;

public class Message {

    public final int id;

    public final int threadId;
    public final int authorId;

    public final Date date;
    public final String rawBody;
    public final Spanned body;

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

    public Message(int id, int threadId, int authorId, Date date, String rawBody, boolean isNew,
                   boolean isPushed) {
        this.id = id;
        this.threadId = threadId;
        this.authorId = authorId;
        this.date = date;
        this.rawBody = stripRawBody(rawBody);
        this.body = parseBody(this.rawBody);
        this.isNew = isNew;
        this.isPushed = isPushed;
    }

    public Message cloneForIsNew(boolean isNew) {
        return new Message(id, threadId, authorId, date, rawBody, isNew, isPushed);
    }
    public Message cloneForIsPushed(boolean isPushed) {
        return new Message(id, threadId, authorId, date, rawBody, isNew, isPushed);
    }

    /**
     * Removes the <p>...</p>\r\n surrounding all messages sent from the web interface.
     */
    private static String stripRawBody(String rawBody) {
        if (rawBody.startsWith("<p>") && rawBody.endsWith("</p>\r\n")) {
            rawBody = rawBody.substring(3, rawBody.length()-6);
        }
        return rawBody;
    }
    /**
     * Replaces \n by <br/> and then returns the html-parsed body.
     */
    private static Spanned parseBody(String rawBody) {
        rawBody = rawBody.replace("\n", "<br/>");
        return Html.fromHtml(rawBody);
    }
}
