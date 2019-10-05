package fi.bitrite.android.ws.persistence.schema.migrations.account;

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;

@AccountScope
public class MigrationTo3 {
    @Inject
    MigrationTo3() {
    }

    void addMessageDraftTable(@NonNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE message_draft (" +
                   "thread_id INTEGER NOT NULL, " +
                   "body TEXT NOT NULL, " +

                   "PRIMARY KEY(thread_id), " +
                   "FOREIGN KEY(thread_id) REFERENCES message_thread(id) " +
                   "  ON UPDATE CASCADE ON DELETE CASCADE " +
                   ")");
    }
}
