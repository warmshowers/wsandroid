package fi.bitrite.android.ws.persistence.db;

import android.content.Context;

import fi.bitrite.android.ws.persistence.schema.SchemaDefinition;

abstract class Database {
    private final LockableDatabase mDatabase;

    Database(Context context, SchemaDefinition schemaDefinition) {
        mDatabase = new LockableDatabase(context, schemaDefinition);
    }

    public void compact() {
        mDatabase.executeNonTransactional(db -> {
            db.execSQL("VACUUM");
            return null;
        });
    }

    public LockableDatabase getDatabase() {
        return mDatabase;
    }
}
