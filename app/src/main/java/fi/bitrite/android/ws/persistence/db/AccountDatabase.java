package fi.bitrite.android.ws.persistence.db;

import android.content.Context;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.persistence.schema.AccountSchemaDefinition;

/**
 * Database that contains information specific to a logged in user.
 */
@AccountScope
public class AccountDatabase extends Database {
    private final static String DB_NAME = "wsandroid_account-%d.db";

    @Inject
    public AccountDatabase(Context context, AccountSchemaDefinition schemaDefinition,
                           @Named("accountUserId") int accountUserId) {
        super(context, schemaDefinition);

        if (accountUserId == AccountManager.UNKNOWN_USER_ID) {
            throw new IllegalStateException("Unknown user id");
        }

        final String dbName = String.format(Locale.US, DB_NAME, accountUserId);
        getDatabase().open(dbName);
    }
}
