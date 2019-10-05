package fi.bitrite.android.ws.model;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import fi.bitrite.android.ws.util.Pushable;

public class MessageThread {
    public final int id;

    public final String subject;
    public final Date started;
    public final Pushable<Boolean> isRead;

    public final List<Integer> participantIds;
    public final List<Message> messages;

    public final Date lastUpdated;

    public MessageThread(int id, String subject, Date started, Pushable<Boolean> isRead,
                         List<Integer> participantIds, List<Message> messages, Date lastUpdated) {
        this.id = id;
        this.subject = subject;
        this.started = started;
        this.isRead = isRead;
        this.participantIds = Collections.unmodifiableList(participantIds);
        this.messages = Collections.unmodifiableList(messages);
        this.lastUpdated = lastUpdated;
    }

    public boolean isRead() {
        return this.isRead.value;
    }

    public boolean hasNewMessages() {
        if (isRead()) {
            return false;
        }
        for (Message message : messages) {
            if (message.isNew) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a clone of the given thread except that the readStatus is changed.
     */
    public MessageThread cloneForReadStatus(Pushable<Boolean> newReadStatus) {
        return new MessageThread(id, subject, started, newReadStatus, participantIds, messages,
                                 lastUpdated);
    }
}
