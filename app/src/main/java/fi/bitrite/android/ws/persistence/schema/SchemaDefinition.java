package fi.bitrite.android.ws.persistence.schema;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Locale;

import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.persistence.schema.migrations.Migrations;

public abstract class SchemaDefinition {
    private final static String TAG = SchemaDefinition.class.getName();

    private final Migrations mMigrations;

    SchemaDefinition(Migrations migrations) {
        mMigrations = migrations;
    }

    public final void upgradeDatabase(@NonNull final SQLiteDatabase db) {
        try {
            doDbUpgrade(db);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                throw new Error("Exception while upgrading database", e);
            }

            Log.e(TAG, "Exception while upgrading database. Resetting the DB to v0", e);
            db.setVersion(0);
            doDbUpgrade(db);
        }
    }

    /**
     * Just delete everything in case of a downgrade.
     */
    public final void downgradeDatabase(@NonNull final SQLiteDatabase db) {
        Log.i(TAG, String.format(Locale.US, "Downgrading database from version %d to version %d",
                db.getVersion(), getVersion()));
        db.setVersion(0);
        doDbUpgrade(db);
    }

    private void doDbUpgrade(@NonNull final SQLiteDatabase db) {
        Log.i(TAG, String.format(Locale.US, "Upgrading database from version %d to version %d",
                db.getVersion(), getVersion()));

        db.beginTransaction();
        try {
            runDbUpgrade(db);

            db.setVersion(getVersion());

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (db.getVersion() != getVersion()) {
            throw new RuntimeException("Database upgrade failed!");
        }
    }

    final void migrateDatabase(@NonNull final SQLiteDatabase db) {
        mMigrations.upgradeDatabase(db);
    }

    abstract void runDbUpgrade(@NonNull final SQLiteDatabase db);

    public abstract int getVersion();
}
