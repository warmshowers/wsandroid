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
    }

    @Override
    public int getVersion() {
        return VERSION;
    }
}
