package fi.bitrite.android.ws.persistence.impl;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import fi.bitrite.android.ws.WSAndroidApplication;

public class DbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "wsandroid.db";
    public static final int DB_VERSION = 3;

    public static final String TABLE_HOSTS = "hosts";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DETAILS = "details";
    public static final String COLUMN_UPDATED = "updated";
    public static final String COLUMN_FEEDBACK = "feedback";

    private static final String DATABASE_CREATE = "create table "
            + TABLE_HOSTS + "( " + COLUMN_ID
            + " integer primary key not null, " + COLUMN_NAME
            + " text not null, " + COLUMN_DETAILS
            + " text not null, " + COLUMN_UPDATED
            + " text not null, " + COLUMN_FEEDBACK
            + " text not null );";

    public DbHelper() {
        super(WSAndroidApplication.getAppContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(WSAndroidApplication.TAG,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HOSTS);
        onCreate(db);
    }
}
