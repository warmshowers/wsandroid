package fi.bitrite.android.ws.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.model.Message;
import fi.bitrite.android.ws.model.MessageThread;
import fi.bitrite.android.ws.persistence.converters.DateConverter;
import fi.bitrite.android.ws.persistence.db.AccountDatabase;

@Singleton
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
    }
    private static class ParticipantTable {
        final static String NAME = "message_thread_participant";

        final static String[] DEFAULT_COLUMNS = {
                "thread_id",  "user_id",
        };

        final static int COL_IDX_THREAD_ID = 0;
        final static int COL_IDX_USER_ID = 1;
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

            for (Message message : thread.messages) {
                saveMessage(db, message);
            }

            return null;
        });
    }
    private void saveThread(SQLiteDatabase db, MessageThread thread) {
        ContentValues cv = new ContentValues();
        cv.put("id", thread.id);
        cv.put("subject", thread.subject);
        cv.put("started", DateConverter.dateToLong(thread.started));
        cv.put("read_status", thread.readStatus);
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
        cv.put("body", message.body);
        cv.put("status", message.status);

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
            String threadIdsStr = StringUtils.join(threadIds, ",");
            db.execSQL("DELETE FROM " + ThreadTable.NAME + " WHERE id NOT IN (" + threadIdsStr + ")");
            return null;
        });
    }

    public void deletePushedTemporaryMessages(int threadId) {
        executeTransactional(db -> {
            db.delete(MessageTable.NAME, "thread_id = ? AND status = ? AND id < 0",
                    int2str(threadId, Message.STATUS_SYNCED));
            return null;
        });
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
                c.getInt(ThreadTable.COL_IDX_READ_STATUS), participantIds, messages,
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
                    messages.add(new Message(
                            cursor.getInt(MessageTable.COL_IDX_ID),
                            cursor.getInt(MessageTable.COL_IDX_THREAD_ID),
                            cursor.getInt(MessageTable.COL_IDX_AUTHOR_ID),
                            DateConverter.longToDate(cursor.getLong(MessageTable.COL_IDX_DATE)),
                            cursor.getString(MessageTable.COL_IDX_BODY),
                            cursor.getInt(MessageTable.COL_IDX_STATUS)));
                } while (cursor.moveToNext());
            }
        }
        return messages;
    }
}
