package fi.bitrite.android.ws.persistence;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import fi.bitrite.android.ws.persistence.db.LockableDatabase;

abstract class Dao {
    private final LockableDatabase mDb;

    Dao(LockableDatabase db) {
        mDb = db;
    }

    void insertOrUpdate(SQLiteDatabase db, String table, ContentValues cv, int id) {
        insertOrUpdate(db, table, cv, "id = ?", new String[] { Integer.toString(id) });
    }

    void insertOrUpdate(SQLiteDatabase db, String table, ContentValues cv, String whereClause, String[] whereArgs) {
        long id =  db.insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            // The column already exists -> we do an update.
            db.update(table, cv, whereClause, whereArgs);
        }
    }

    public <T> T executeTransactional(@NonNull final LockableDatabase.DbCallback<T> callback) {
        return mDb.executeTransactional(callback);
    }

    public <T> T executeNonTransactional(@NonNull final LockableDatabase.DbCallback<T> callback) {
        return mDb.executeNonTransactional(callback);
    }

    String[] int2str(int... ints) {
        String[] res = new String[ints.length];
        for (int i = 0; i < ints.length; ++i) {
            res[i] = String.valueOf(ints[i]);
        }
        return res;
    }
}
