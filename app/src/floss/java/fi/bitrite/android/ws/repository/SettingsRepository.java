package fi.bitrite.android.ws.repository;

import android.content.Context;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.AppScope;

@AppScope
public class SettingsRepository extends BaseSettingsRepository {

    @Inject
    SettingsRepository(Context context) {
        super(context);
    }
}
