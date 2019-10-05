package fi.bitrite.android.ws.persistence.schema;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.persistence.schema.migrations.account.AccountMigrations;
import fi.bitrite.android.ws.persistence.schema.migrations.app.MigrationTo4;
import io.reactivex.disposables.Disposable;

@AccountScope
public class AccountSchemaDefinition extends SchemaDefinition {
    private final static int VERSION = 4;

    @Inject
    AccountSchemaDefinition(AccountMigrations accountMigrations) {
        super(accountMigrations);
    }

    @Override
    void runDbUpgrade(@NonNull SQLiteDatabase db) {
        if (db.getVersion() == 0) {
            createDatabaseFromScratch(db);
            recoverSavedFavoriteUserIds(db);
        } else {
            migrateDatabase(db);
        }
    }

    private void createDatabaseFromScratch(@NonNull SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS favorite_user");
        db.execSQL("CREATE TABLE favorite_user (" +
                   "user_id INTEGER NOT NULL, " +

                   "PRIMARY KEY(user_id) " +
                   ")");

        db.execSQL("DROP TABLE IF EXISTS message_thread");
        db.execSQL("CREATE TABLE message_thread (" +
                   "id INTEGER NOT NULL, " +
                   "subject TEXT NOT NULL, " +
                   "started LONG NOT NULL, " +
                   "read_status INTEGER NOT NULL, " +
                   "last_updated INTEGER NOT NULL, " +

                   "PRIMARY KEY(id) " +
                   ")");

        db.execSQL("DROP TABLE IF EXISTS message");
        db.execSQL("CREATE TABLE message (" +
                   "id INTEGER NOT NULL, " +
                   "thread_id INTEGER NOT NULL, " +
                   "author_id INTEGER NOT NULL, " +
                   "date LONG NOT NULL, " +
                   "body TEXT NOT NULL, " +
                   "status INTEGER NOT NULL, " +

                   "PRIMARY KEY(id), " +
                   "FOREIGN KEY(thread_id) REFERENCES message_thread(id) " +
                   "  ON UPDATE CASCADE ON DELETE CASCADE " +
                   ")");

        db.execSQL("DROP TABLE IF EXISTS message_thread_participant");
        db.execSQL("CREATE TABLE message_thread_participant (" +
                   "thread_id INTEGER NOT NULL, " +
                   "user_id INTEGER NOT NULL, " +

                   "PRIMARY KEY(thread_id, user_id), " +
                   "FOREIGN KEY(thread_id) REFERENCES message_thread(id) " +
                   "  ON UPDATE CASCADE ON DELETE CASCADE " +
                   ")");

        db.execSQL("DROP TABLE IF EXISTS message_draft");
        db.execSQL("CREATE TABLE message_draft (" +
                   "thread_id INTEGER NOT NULL, " +
                   "body TEXT NOT NULL, " +

                   "PRIMARY KEY(thread_id), " +
                   "FOREIGN KEY(thread_id) REFERENCES message_thread(id) " +
                   "  ON UPDATE CASCADE ON DELETE CASCADE " +
                   ")");
    }

    private void recoverSavedFavoriteUserIds(@NonNull SQLiteDatabase db) {
        Disposable unused = MigrationTo4.savedFavoriteUserIds.subscribe(favoriteUserIds -> {
            ContentValues cv = new ContentValues();
            for (Integer userId : favoriteUserIds) {
                cv.put("user_id", userId);
                db.insert("favorite_user", null, cv);
            }

            // Mark the savedFavoriteUserIds as completed s.t. no other account imports the
            // favorites.
            MigrationTo4.savedFavoriteUserIds.onComplete();
        });
    }

    @Override
    public int getVersion() {
        return VERSION;
    }
}
