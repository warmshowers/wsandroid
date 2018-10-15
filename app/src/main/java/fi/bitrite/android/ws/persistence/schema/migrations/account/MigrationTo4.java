package fi.bitrite.android.ws.persistence.schema.migrations.account;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;

@AccountScope
public class MigrationTo4 {
    @Inject
    MigrationTo4() {
    }

    /**
     * Previously, we removed the first "<p>" and the last "</p>\r\n" from each message body.
     * This undoes that modification.
     */
    void reSurroundMessageBodiesWithParagraphTag(@NonNull SQLiteDatabase db) {
        db.execSQL("UPDATE message SET body = '<p>' || body || '</p>\r\n' "
                   + "WHERE body LIKE '%</p>%' "
                   + "  OR body LIKE '%<br />%'"
                   + "  OR body LIKE '%<s>%'"
                   + "  OR body LIKE '%<strong>%'");
    }
}
