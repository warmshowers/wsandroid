package fi.bitrite.android.ws.repository;

import android.content.Context;
import android.content.res.Resources;

import javax.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.di.AppScope;

@AppScope
public class SettingsRepository extends BaseSettingsRepository {
    private final String mKeyGaCollectStats;
    private final boolean mDefaultGaCollectStats;

    @Inject
    SettingsRepository(Context context) {
        super(context);

        final Resources res = context.getResources();
        mKeyGaCollectStats = res.getString(R.string.prefs_ga_collect_stats_key);
        mDefaultGaCollectStats = res.getBoolean(R.bool.prefs_ga_collect_stats_default);
    }

    public boolean canCollectStats() {
        return mSharedPreferences.getBoolean(mKeyGaCollectStats, mDefaultGaCollectStats);
    }
    public String getCanCollectStatsKey() {
        return mKeyGaCollectStats;
    }
}
