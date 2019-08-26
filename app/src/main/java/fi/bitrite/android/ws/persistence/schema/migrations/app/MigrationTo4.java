package fi.bitrite.android.ws.persistence.schema.migrations.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.AppScope;
import io.reactivex.subjects.BehaviorSubject;

@AppScope
public class MigrationTo4 {
    public static final BehaviorSubject<List<Integer>> savedFavoriteUserIds =
            BehaviorSubject.create();

    @Inject
    MigrationTo4() {
    }

    void moveHostsToUsers(@NonNull SQLiteDatabase db) {
        // Creates the users table.
        db.execSQL("CREATE TABLE user (" +
                   "id INTEGER NOT NULL, " +
                   "name TEXT, " +
                   "fullname TEXT, " +
                   "street TEXT, " +
                   "additional_address TEXT, " +
                   "city TEXT, " +
                   "province TEXT, " +
                   "postal_code TEXT, " +
                   "country_code TEXT, " +
                   "mobile_phone TEXT, " +
                   "home_phone TEXT, " +
                   "work_phone TEXT, " +
                   "comments TEXT, " +
                   "preferred_notice TEXT, " +
                   "max_cyclists_count INTEGER NOT NULL, " +
                   "distance_to_motel TEXT, " +
                   "distance_to_campground TEXT, " +
                   "distance_to_bikeshop TEXT, " +
                   "has_storage INTEGER NOT NULL, " +
                   "has_shower INTEGER NOT NULL, " +
                   "has_kitchen INTEGER NOT NULL, " +
                   "has_lawnspace INTEGER NOT NULL, " +
                   "has_sag INTEGER NOT NULL, " +
                   "has_bed INTEGER NOT NULL, " +
                   "has_laundry INTEGER NOT NULL, " +
                   "has_food INTEGER NOT NULL, " +
                   "last_access INTEGER, " +
                   "created INTEGER, " +
                   "currently_available INTEGER NOT NULL, " +
                   "spoken_languages TEXT, " +
                   "latitude REAL, " +
                   "longitude REAL, " +
                   "profile_picture_small TEXT, " +
                   "profile_picture_large TEXT, " +

                   "PRIMARY KEY(id) " +
                   ")");

        // Moves the users from the old hosts table and marks them as favorites in the new one.
        // We do not copy any user data as this is simply refetched the next time we look at the
        // favorites list.
        // To avoid circular dependencies, we store the favorite userIds in a static field which is
        // later processed by the account migration helper.
        List<Integer> favoriteUserIds = new ArrayList<>();
        try (Cursor cursor = db.query("hosts", new String[]{ "_id" }, null, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                do {
                    final int userId = cursor.getInt(0);
                    favoriteUserIds.add(userId);
                } while (cursor.moveToNext());
            }
        }
        savedFavoriteUserIds.onNext(favoriteUserIds);

        // Drops the old hosts table.
        db.execSQL("DROP TABLE IF EXISTS hosts");
    }

    void createFeedbackTable(@NonNull SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS feedback");
        db.execSQL("CREATE TABLE feedback (" +
                   "id INTEGER NOT NULL, " +
                   "recipient_id INTEGER NOT NULL, " +
                   "sender_id INTEGER NOT NULL, " +
                   "sender_fullname TEXT, " +
                   "relation INTEGER, " +
                   "rating INTEGER, " +
                   "meeting_date INTEGER, " +
                   "body TEXT, " +

                   "PRIMARY KEY(id), " +
                   "FOREIGN KEY(recipient_id) REFERENCES user(id) " +
                   "  ON UPDATE NO ACTION ON DELETE CASCADE " +
                   ")");
        db.execSQL("CREATE INDEX index_feedback_sender_id ON feedback(sender_id)");
        db.execSQL("CREATE INDEX index_feedback_recipient_id ON feedback(recipient_id)");
    }
}
