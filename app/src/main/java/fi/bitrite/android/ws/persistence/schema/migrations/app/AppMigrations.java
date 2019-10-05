package fi.bitrite.android.ws.persistence.schema.migrations.app;

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.AppScope;
import fi.bitrite.android.ws.persistence.schema.migrations.Migrations;

@AppScope
public class AppMigrations implements Migrations {
    @Inject MigrationTo4 mMigrationTo4;

    @Inject
    AppMigrations() {
    }

    /**
     * Runs the required updates to reach the new db version. A transaction is already started.
     */
    @SuppressWarnings("fallthrough")
    public void upgradeDatabase(@NonNull SQLiteDatabase db) {
        switch (db.getVersion()) {
            case 3:
                mMigrationTo4.moveHostsToUsers(db);
                mMigrationTo4.createFeedbackTable(db);
        }
    }
}
