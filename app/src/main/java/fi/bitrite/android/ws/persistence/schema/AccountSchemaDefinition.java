package fi.bitrite.android.ws.persistence.schema;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.persistence.schema.migrations.account.AccountMigrations;

@Singleton
public class AccountSchemaDefinition extends SchemaDefinition {
    private final static int VERSION = 1;

    @Inject
    AccountSchemaDefinition(AccountMigrations accountMigrations) {
        super(accountMigrations);
    }

    @Override
    void runDbUpgrade(@NonNull SQLiteDatabase db) {
        if (db.getVersion() == 0) {
            createDatabaseFromScratch(db);
        }

        migrateDatabase(db);
    }

    @Override
    void createDatabaseFromScratch(@NonNull SQLiteDatabase db) {
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
                "FOREIGN KEY(thread_id) REFERENCES message_thread(id) ON UPDATE CASCADE ON DELETE CASCADE " +
                ")");

        db.execSQL("DROP TABLE IF EXISTS message_thread_participant");
        db.execSQL("CREATE TABLE message_thread_participant (" +
                "thread_id INTEGER NOT NULL, " +
                "user_id INTEGER NOT NULL, " +

                "PRIMARY KEY(thread_id, user_id), " +
                "FOREIGN KEY(thread_id) REFERENCES message_thread(id) ON UPDATE CASCADE ON DELETE CASCADE " +
                ")");
    }

    @Override
    public int getVersion() {
        return VERSION;
    }
}
