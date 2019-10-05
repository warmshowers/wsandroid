package fi.bitrite.android.ws.persistence.schema.migrations;

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

public interface Migrations {
    void upgradeDatabase(@NonNull SQLiteDatabase db);
}
