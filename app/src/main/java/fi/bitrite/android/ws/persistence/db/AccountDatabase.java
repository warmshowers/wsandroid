package fi.bitrite.android.ws.persistence.db;


import android.content.Context;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.persistence.schema.AccountSchemaDefinition;

/**
 * Database that contains information specific to a logged in user.
 */
@Singleton
public class AccountDatabase extends Database {
    private final static String DB_NAME = "wsandroid_account-%d.db";

    @Inject
    public AccountDatabase(Context context, AccountSchemaDefinition schemaDefinition,
                           AccountManager accountManager) {
        super(context, schemaDefinition);

        accountManager.getCurrentAccountId().subscribe(accountId -> {
            // Closes the current db (if it is open).
            getDatabase().close();

            if (accountId > 0) {
                // Opens the db for the new accountId.
                getDatabase().open(getDbName(accountId));
            }
        });
    }

    private static String getDbName(int accountId) {
        return String.format(Locale.US, DB_NAME, accountId);
    }
}
