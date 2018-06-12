package fi.bitrite.android.ws.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.persistence.db.AccountDatabase;

@AccountScope
public class FavoriteDao extends Dao {
    private final static String TABLE_NAME = "favorite_user";

    private final FeedbackDao mFeedbackDao;
    private final UserDao mUserDao;

    @Inject
    FavoriteDao(AccountDatabase db, FeedbackDao feedbackDao, UserDao userDao) {
        super(db.getDatabase());

        mFeedbackDao = feedbackDao;
        mUserDao = userDao;
    }

    /**
     * Returns the ids of the user's favorites.
     */
    public List<Integer> loadAll() {
        return executeNonTransactional(db -> {
            List<Integer> favoriteUserIds = new ArrayList<>();

            try (Cursor cursor = db.query(TABLE_NAME, new String[]{ "user_id" },
                    null, null, null, null, null)) {

                if (cursor.moveToFirst()) {
                    do {
                        int userId = cursor.getInt(0);
                        favoriteUserIds.add(userId);
                    } while (cursor.moveToNext());
                }
            }

            return favoriteUserIds;
        });
    }

    public boolean exists(int userId) {
        return executeNonTransactional(db -> {
            try (Cursor c = db.query(TABLE_NAME, null, "user_id = ?", int2str(userId),
                    null, null, null, null)) {
                return c.moveToFirst();
            }
        });
    }

    public void add(int userId) {
        executeNonTransactional(db -> {
            ContentValues cv = new ContentValues();
            cv.put("user_id", userId);

            // Ignore conflicts as we only want to have it there. There are also no other columns
            // that need an update.
            db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE);

            return null;
        });
    }

    public void add(@NonNull User user, @Nullable List<Feedback> receivedFeedbacks) {
        // Saves the user.
        mUserDao.executeTransactional(db -> {
            // They are in the same DB so we can save the user and their feedback transactional.
            mUserDao.save(db, user);
            mFeedbackDao.saveForRecipient(db, user.id, receivedFeedbacks);

            return null;
        });

        // Marks the user as a favorite.
        add(user.id);
    }

    public void remove(int userId) {
        // TODO(saemy): Also remove it from the users table (But only if e.g. no messages).
        executeTransactional(db -> {
            db.delete(TABLE_NAME, "user_id = ?", int2str(userId));
            mFeedbackDao.removeAllForRecipient(userId);

            return null;
        });
    }
}
