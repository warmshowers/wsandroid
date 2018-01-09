package fi.bitrite.android.ws.persistence.schema;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.persistence.schema.migrations.app.AppMigrations;

@Singleton
public class AppSchemaDefinition extends SchemaDefinition {
    private final static int VERSION = 4;

    @Inject
    AppSchemaDefinition(AppMigrations appMigrations) {
        super(appMigrations);
    }

    @Override
    void runDbUpgrade(@NonNull SQLiteDatabase db) {
        if (db.getVersion() < 3) {
            createDatabaseFromScratch(db);
        } else {
            migrateDatabase(db);
        }
    }

    @Override
    void createDatabaseFromScratch(@NonNull SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS user");
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
                "FOREIGN KEY(recipient_id) REFERENCES user(id) ON UPDATE NO ACTION ON DELETE CASCADE " +
                ")");
        db.execSQL("CREATE INDEX index_feedback_recipient_id ON feedback(recipient_id)");
        db.execSQL("CREATE INDEX index_feedback_sender_id ON feedback(sender_id)");
    }

    @Override
    public int getVersion() {
        return VERSION;
    }
}
