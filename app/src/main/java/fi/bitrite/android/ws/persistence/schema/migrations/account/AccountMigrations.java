package fi.bitrite.android.ws.persistence.schema.migrations.account;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.persistence.schema.migrations.Migrations;

@Singleton
public class AccountMigrations implements Migrations {
    @Inject MigrationTo1 mMigrationTo1;

    @Inject
    AccountMigrations() {
    }

    /**
     * Runs the required updates to reach the new db version. A transaction is already started.
     */
    @SuppressWarnings("fallthrough")
    public void upgradeDatabase(@NonNull SQLiteDatabase db) {
        switch (db.getVersion()) {
            case 0: mMigrationTo1.recoverSavedFavoriteUserIds(db);
        }
    }
}
