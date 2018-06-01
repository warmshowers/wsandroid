package fi.bitrite.android.ws.persistence.db;

import android.content.Context;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.AppScope;
import fi.bitrite.android.ws.persistence.schema.AppSchemaDefinition;

/**
 * Database that contains data for the whole application, regardless of which account is logged in.
 */
@AppScope
public class AppDatabase extends Database {
    private final static String DB_NAME = "wsandroid.db";

    @Inject
    public AppDatabase(Context context, AppSchemaDefinition schemaDefinition) {
        super(context, schemaDefinition);
        getDatabase().open(DB_NAME);
    }
}
