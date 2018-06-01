package fi.bitrite.android.ws.persistence.schema.migrations.account;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.persistence.schema.migrations.Migrations;

@AccountScope
public class AccountMigrations implements Migrations {
    @Inject MigrationTo1 mMigrationTo1;
    @Inject MigrationTo2 mMigrationTo2;

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
            case 1: mMigrationTo2.fixMessageStatus(db);
        }
    }
}
