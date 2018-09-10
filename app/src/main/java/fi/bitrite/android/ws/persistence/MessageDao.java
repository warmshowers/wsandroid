package fi.bitrite.android.ws.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.Message;
import fi.bitrite.android.ws.model.MessageThread;
import fi.bitrite.android.ws.persistence.converters.DateConverter;
import fi.bitrite.android.ws.persistence.converters.PushableConverter;
import fi.bitrite.android.ws.persistence.db.AccountDatabase;

@AccountScope
public class MessageDao extends Dao {
    private static class ThreadTable {
        final static String NAME = "message_thread";

        final static String[] DEFAULT_COLUMNS = {
                "id", "subject", "started", "read_status", "last_updated",
        };

        final static int COL_IDX_ID = 0;
        final static int COL_IDX_SUBJECT = 1;
        final static int COL_IDX_STARTED = 2;
        final static int COL_IDX_READ_STATUS = 3;
        final static int COL_IDX_LAST_UPDATED = 4;
    }
    private static class MessageTable {
        final static String NAME = "message";

        final static String[] DEFAULT_COLUMNS = {
                "id", "thread_id", "author_id", "date", "body", "status",
        };

        final static int COL_IDX_ID = 0;
        final static int COL_IDX_THREAD_ID = 1;
        final static int COL_IDX_AUTHOR_ID = 2;
        final static int COL_IDX_DATE = 3;
        final static int COL_IDX_BODY = 4;
        final static int COL_IDX_STATUS = 5;

        /* Whether this message is new and a notification should be shown for it. */
        private final static byte STATUS_BIT_IS_NEW = 0;
        /* Whether this message is already pushed to the webservice. */
        private final static byte STATUS_BIT_IS_PUSHED = 1;
    }
    private static class ParticipantTable {
        final static String NAME = "message_thread_participant";

        final static String[] DEFAULT_COLUMNS = {
                "thread_id", "user_id",
        };

        final static int COL_IDX_THREAD_ID = 0;
        final static int COL_IDX_USER_ID = 1;
    }

    private static class MessageDraftTable {
        final static String NAME = "message_draft";

        final static String[] DEFAULT_COLUMNS = {
                "thread_id", "body",
        };

        final static int COL_IDX_THREAD_ID = 0;
        final static int COL_IDX_BODY = 1;
    }

    @Inject
    MessageDao(AccountDatabase db) {
        super(db.getDatabase());
    }

    public MessageThread loadThread(int threadId) {
        return executeTransactional(db -> {
            try (Cursor cursor = db.query(ThreadTable.NAME, ThreadTable.DEFAULT_COLUMNS, "id = ?",
                    int2str(threadId), null, null, null, null)) {

                if (!cursor.moveToFirst()) {
                    return null;
                }

                return getThreadFromCursor(db, cursor);
            }
        });

    }

    public List<MessageThread> loadAll() {
        return executeTransactional(db -> {
            List<MessageThread> threads = new ArrayList<>();

            try (Cursor cursor = db.query(ThreadTable.NAME, ThreadTable.DEFAULT_COLUMNS,
                    null, null, null, null, null, null)) {

                if (cursor.moveToFirst()) {
                    do {
                        MessageThread thread = getThreadFromCursor(db, cursor);
                        threads.add(thread);
                    } while (cursor.moveToNext());
                }
            }

            return threads;
        });
    }

    public void save(MessageThread thread) {
        executeTransactional(db -> {
            saveThread(db, thread);

            for (Integer participantId : thread.participantIds) {
                saveParticipant(db, thread.id, participantId);
            }
            deleteParticipantsExcept(db, thread.id, thread.participantIds);

            List<Integer> messageIds = new ArrayList<>(thread.messages.size());
            // FIXME: got java.util.ConcurrentModificationException, coming from MessageRepository.java:278
            for (Message message : thread.messages) {
                messageIds.add(message.id);
                saveMessage(db, message);
            }
            deleteMessagesExcept(db, thread.id, messageIds);

            return null;
        });
    }
    private void saveThread(SQLiteDatabase db, MessageThread thread) {
        ContentValues cv = new ContentValues();
        cv.put("id", thread.id);
        cv.put("subject", thread.subject);
        cv.put("started", DateConverter.dateToLong(thread.started));
        cv.put("read_status", PushableConverter.pushableToInt(thread.isRead));
        cv.put("last_updated", DateConverter.dateToLong(thread.lastUpdated));

        insertOrUpdate(db, ThreadTable.NAME, cv, thread.id);
    }
    private void saveParticipant(SQLiteDatabase db, int threadId, int userId) {
        ContentValues cv = new ContentValues();
        cv.put("thread_id", threadId);
        cv.put("user_id", userId);

        insertOrUpdate(db, ParticipantTable.NAME, cv, "thread_id = ? AND user_id = ?",
                int2str(threadId, userId));
    }
    private void saveMessage(SQLiteDatabase db, Message message) {
        ContentValues cv = new ContentValues();
        cv.put("id", message.id);
        cv.put("thread_id", message.threadId);
        cv.put("author_id", message.authorId);
        cv.put("date", DateConverter.dateToLong(message.date));
        cv.put("body", message.rawBody);
        int status = (message.isNew ? 1 : 0) << MessageTable.STATUS_BIT_IS_NEW |
                (message.isPushed ? 1 : 0) << MessageTable.STATUS_BIT_IS_PUSHED;
        cv.put("status", status);

        insertOrUpdate(db, MessageTable.NAME, cv, message.id);
    }

    /**
     * This should only be used to modify the status of a message.
     */
    public void saveMessage(Message message) {
        executeTransactional(db -> {
            saveMessage(db, message);
            return null;
        });
    }

    public void delete(int threadId) {
        executeTransactional(db -> {
            // ON DELETE takes care of deleting the messages.
            db.delete(ThreadTable.NAME, "id = ?", int2str(threadId));
            return null;
        });
    }
    public void deleteExcept(List<Integer> threadIds) {
        if (threadIds.isEmpty()) {
            return;
        }

        executeTransactional(db -> {
            String threadIdsStr = TextUtils.join(",", threadIds);
            db.execSQL("DELETE FROM " + ThreadTable.NAME +
                       " WHERE id NOT IN (" + threadIdsStr + ")");
            return null;
        });
    }

    private void deleteParticipantsExcept(SQLiteDatabase db, int threadId,
                                          Collection<Integer> participantIds) {
        String participantIdsStr = TextUtils.join(",", participantIds);
        db.execSQL("DELETE FROM " + ParticipantTable.NAME +
                   " WHERE thread_id = " + threadId +
                   " AND user_id NOT IN (" + participantIdsStr + ")");
    }

    private void deleteMessagesExcept(SQLiteDatabase db, int threadId,
                                      Collection<Integer> messageIds) {
        String messageIdsStr = TextUtils.join(",", messageIds);
        db.execSQL("DELETE FROM " + MessageTable.NAME +
                   " WHERE thread_id = " + threadId +
                   " AND id NOT IN (" + messageIdsStr + ")");
    }

    private static MessageThread getThreadFromCursor(@NonNull SQLiteDatabase db,
                                                     @NonNull Cursor c) {
        final int threadId = c.getInt(ThreadTable.COL_IDX_ID);

        List<Integer> participantIds = loadParticipants(db, threadId);
        List<Message> messages = loadMessages(db, threadId);

        return new MessageThread(
                threadId,
                c.getString(ThreadTable.COL_IDX_SUBJECT),
                DateConverter.longToDate(c.getLong(ThreadTable.COL_IDX_STARTED)),
                PushableConverter.intToBooleanPushable(c.getInt(ThreadTable.COL_IDX_READ_STATUS)),
                participantIds, messages,
                DateConverter.longToDate(c.getLong(ThreadTable.COL_IDX_LAST_UPDATED)));
    }
    private static List<Integer> loadParticipants(@NonNull SQLiteDatabase db, int threadId) {
        List<Integer> participantIds = new LinkedList<>();
        try (Cursor cursor = db.query(ParticipantTable.NAME, ParticipantTable.DEFAULT_COLUMNS,
                "thread_id = ?", int2str(threadId), null, null, null, null)) {
            if (cursor.moveToFirst()) {
                do {
                    participantIds.add(cursor.getInt(ParticipantTable.COL_IDX_USER_ID));
                } while (cursor.moveToNext());
            }
        }
        return participantIds;
    }
    private static List<Message> loadMessages(@NonNull SQLiteDatabase db, int threadId) {
        List<Message> messages = new LinkedList<>();
        try (Cursor cursor = db.query(MessageTable.NAME, MessageTable.DEFAULT_COLUMNS,
                "thread_id = ?", int2str(threadId), null, null, null, null)) {
            if (cursor.moveToFirst()) {
                do {
                    int status = cursor.getInt(MessageTable.COL_IDX_STATUS);
                    boolean isNew = (status & (1 << MessageTable.STATUS_BIT_IS_NEW)) != 0;
                    boolean isPushed = (status & (1 << MessageTable.STATUS_BIT_IS_PUSHED)) != 0;

                    messages.add(new Message(
                            cursor.getInt(MessageTable.COL_IDX_ID),
                            cursor.getInt(MessageTable.COL_IDX_THREAD_ID),
                            cursor.getInt(MessageTable.COL_IDX_AUTHOR_ID),
                            DateConverter.longToDate(cursor.getLong(MessageTable.COL_IDX_DATE)),
                            cursor.getString(MessageTable.COL_IDX_BODY),
                            isNew, isPushed));
                } while (cursor.moveToNext());
            }
        }
        return messages;
    }

    public String getAndDeleteDraft(int threadId) {
        return executeTransactional(db -> {
            String body = "";
            try (Cursor cursor = db.query(MessageDraftTable.NAME, MessageDraftTable.DEFAULT_COLUMNS,
                    "thread_id = ?", int2str(threadId), null, null, null, null)) {
                if (cursor.moveToFirst()) {
                    body = cursor.getString(MessageDraftTable.COL_IDX_BODY);
                    db.delete(MessageDraftTable.NAME, "thread_id = ?", int2str(threadId));
                }
            }
            return body;
        });
    }

    public void saveDraft(int threadId, String body) {
        executeTransactional(db -> {
            ContentValues cv = new ContentValues();
            cv.put("thread_id", threadId);
            cv.put("body", body);

            insertOrUpdate(db, MessageDraftTable.NAME, cv, threadId);
            return null;
        });
    }
}
