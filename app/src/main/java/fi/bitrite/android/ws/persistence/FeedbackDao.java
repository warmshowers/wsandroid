package fi.bitrite.android.ws.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.AppScope;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.persistence.converters.DateConverter;
import fi.bitrite.android.ws.persistence.converters.RatingConverter;
import fi.bitrite.android.ws.persistence.converters.RelationConverter;
import fi.bitrite.android.ws.persistence.db.AppDatabase;

@AppScope
public class FeedbackDao extends Dao {
    private final static String TABLE_NAME = "feedback";

    private final static String[] DEFAULT_COLUMNS = {
            "id", "recipient_id", "sender_id", "sender_fullname", "relation", "rating",
            "meeting_date", "body",
    };

    private final static int COL_IDX_ID = 0;
    private final static int COL_IDX_RECIPIENT_ID = 1;
    private final static int COL_IDX_SENDER_ID = 2;
    private final static int COL_IDX_SENDER_FULLNAME = 3;
    private final static int COL_IDX_RELATION = 4;
    private final static int COL_IDX_RATING = 5;
    private final static int COL_IDX_MEETING_DATE = 6;
    private final static int COL_IDX_BODY = 7;

    @Inject
    public FeedbackDao(AppDatabase db) {
        super(db.getDatabase());
    }

    public List<Feedback> loadByRecipient(int recipientId) {
        return executeNonTransactional(db -> {
            try (Cursor cursor = db.query(TABLE_NAME, DEFAULT_COLUMNS,
                    "recipient_id = ?", int2str(recipientId),
                    null, null, null, null)) {
                return parseFeedbackList(cursor);
            }
        });
    }

    public List<Feedback> loadBySender(int senderId) {
        return executeNonTransactional(db -> {

            try (Cursor cursor = db.query(TABLE_NAME, DEFAULT_COLUMNS,
                    "sender_id = ?", new String[]{ Integer.toString(senderId) },
                    null, null, null)) {

                return parseFeedbackList(cursor);
            }
        });
    }

    public void saveForSender(int senderId, List<Feedback> feedback) {
        executeTransactional(db -> {
            db.delete(TABLE_NAME, "sender_id = ?", int2str(senderId));

            save(db, feedback);

            return null;
        });
    }
    public void saveForRecipient(int recipientId, List<Feedback> feedback) {
        executeTransactional(db -> {
            saveForRecipient(db, recipientId, feedback);
            return null;
        });
    }
    public void saveForRecipient(SQLiteDatabase db, int recipientId,
                                 @Nullable List<Feedback> feedback) {
        db.delete(TABLE_NAME, "recipient_id = ?", int2str(recipientId));

        save(db, feedback);
    }
    private void save(SQLiteDatabase db, @Nullable List<Feedback> feedbacks) {
        if (feedbacks == null) {
            return;
        }

        ContentValues cv = new ContentValues();
        for (Feedback feedback : feedbacks) {
            cv.put("id", feedback.id);
            cv.put("recipient_id", feedback.recipientId);
            cv.put("sender_id", feedback.senderId);
            cv.put("sender_fullname", feedback.senderFullname);
            cv.put("relation", RelationConverter.relationToInt(feedback.relation));
            cv.put("rating", RatingConverter.ratingToInt(feedback.rating));
            cv.put("meeting_date", DateConverter.dateToLong(feedback.meetingDate));
            cv.put("body", feedback.body);

            insertOrUpdate(db, TABLE_NAME, cv, feedback.id);
        }
    }

    public void removeAllForRecipient(int recipientId) {
        executeNonTransactional(db -> {
            removeAllForRecipient(db, recipientId);
            return null;
        });
    }
    public void removeAllForRecipient(SQLiteDatabase db, int recipientId) {
        db.delete(TABLE_NAME, "recipient_id = ?", int2str(recipientId));
    }

    private static List<Feedback> parseFeedbackList(@NonNull Cursor cursor) {
        List<Feedback> feedbacks = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Feedback feedback = getFeedbackFromCursor(cursor);
                feedbacks.add(feedback);
            } while (cursor.moveToNext());
        }
        return feedbacks;
    }
    private static Feedback getFeedbackFromCursor(@NonNull Cursor c) {
        return new Feedback(
                c.getInt(COL_IDX_ID), c.getInt(COL_IDX_RECIPIENT_ID), c.getInt(COL_IDX_SENDER_ID),
                c.getString(COL_IDX_SENDER_FULLNAME),
                RelationConverter.intToRelation(c.getInt(COL_IDX_RELATION)),
                RatingConverter.intToRating(c.getInt(COL_IDX_RATING)),
                DateConverter.longToDate(c.getLong(COL_IDX_MEETING_DATE)),
                c.getString(COL_IDX_BODY));
    }
}
