package fi.bitrite.android.ws.persistence.schema.migrations.account;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MigrationTo2 {
    @Inject
    MigrationTo2() {
    }

    /**
     * Migration between two developer versions. This is however needed, as one is already pushed
     * and moving between them would otherwise re-send every single message in the whole inbox.
     * Whoops... ;)
     */
    void fixMessageStatus(@NonNull SQLiteDatabase db) {
        // Fixes the status of the message.
        // Old: 0 for "synced" and 1 for "toBeSent".
        // New: Bit 0: Whether the message is new and a notification should be shown.
        //      Bit 1: Whether the message is pushed to the webservice.
        //
        // We simply ignore any status and just set it to "pushed".
        db.execSQL("UPDATE message SET status=2");
    }
}
