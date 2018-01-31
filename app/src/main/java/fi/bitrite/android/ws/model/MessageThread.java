package fi.bitrite.android.ws.model;

import java.util.Date;
import java.util.List;

public class MessageThread {
    public final static int STATUS_READ = 0;
    public final static int STATUS_UNREAD = 1;

    // The following status are only needed by the MessageRepository to mark a message as (un)read
    // while there was an error pushing the update to the webservice.
    public final static int STATUS_READ_NOT_YET_PUSHED = 2;
    public final static int STATUS_UNREAD_NOT_YET_PUSHED = 3;


    public final int id;

    public final String subject;
    public final Date started;
    public final int readStatus;

    public final List<Integer> participantIds;
    public final List<Message> messages;

    public final Date lastUpdated;

    public MessageThread(int id, String subject, Date started, boolean isUnread,
                         List<Integer> participantIds, List<Message> messages, Date lastUpdated) {
        this(id, subject, started, isUnread ? STATUS_UNREAD : STATUS_READ, participantIds, messages,
                lastUpdated);
    }
    public MessageThread(int id, String subject, Date started, int readStatus,
        List<Integer> participantIds, List<Message> messages, Date lastUpdated) {
        this.id = id;
        this.subject = subject;
        this.started = started;
        this.readStatus = readStatus;
        this.participantIds = participantIds;
        this.messages = messages;
        this.lastUpdated = lastUpdated;
    }

    public boolean isUnread() {
        return readStatus == STATUS_UNREAD || readStatus == STATUS_UNREAD_NOT_YET_PUSHED;
    }
    public boolean isRead() {
        return readStatus == STATUS_READ || readStatus == STATUS_READ_NOT_YET_PUSHED;
    }
}
