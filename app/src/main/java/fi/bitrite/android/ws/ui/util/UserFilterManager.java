package fi.bitrite.android.ws.ui.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.repository.FavoriteRepository;
import fi.bitrite.android.ws.repository.SettingsRepository;


/**
 * Manages filters for users displayed in MapFragment.
 */
@AccountScope
public class UserFilterManager {
    public static final String RECENT_ACTIVITY_FILTER_KEY = "recentActivityFilter";
    public static final String CURRENTLY_AVAILABLE_FILTER_KEY = "currentlyAvailableFilter";
    public static final String FAVORITE_USER_FILTER_KEY = "favoriteUser";

    private final HashMap<String, UserFilter> mFilters = new HashMap<>();

    private SettingsRepository mSettingsRepository;
    private FavoriteRepository mFavoriteRepository;

    @Inject
    UserFilterManager(SettingsRepository settingsRepository, FavoriteRepository favoriteRepository) {
        mSettingsRepository = settingsRepository;
        mFavoriteRepository = favoriteRepository;

        mFilters.put(RECENT_ACTIVITY_FILTER_KEY, new RecentActivityFilter());
        mFilters.put(CURRENTLY_AVAILABLE_FILTER_KEY, new CurrentlyAvailableFilter());
        mFilters.put(FAVORITE_USER_FILTER_KEY, new FavoriteUserFilter());

        // Restore the filters from the last run.
        load();
    }

    public boolean filterUser(SimpleUser user) {
        for (UserFilter filter : mFilters.values()) {
            if (!filter.filterUser(user)) {
                return false;
            }
        }
        return true;
    }

    public boolean isAnyFilterActive() {
        for (UserFilter filter : mFilters.values()) {
            if (filter.isActive()) {
                return true;
            }
        }
        return false;
    }

    public class InvalidFilterException extends RuntimeException { }

    public void setFilterActivated(String filterKey, boolean activated) {
        try {
            mFilters.get(filterKey).setActive(activated);
        } catch (NullPointerException e) {
            throw new InvalidFilterException();
        }
    }

    public void activateFilter(String filterKey) {
        setFilterActivated(filterKey, true);
    }

    public void deactivateFilter(String filterKey) {
        setFilterActivated(filterKey, false);
    }

    public boolean isFilterActive(String filterKey) {
        try {
            return mFilters.get(filterKey).isActive();
        } catch (NullPointerException e) {
            throw new InvalidFilterException();
        }
    }

    public void updateFilterValue(String filterKey, int value) {
        try {
            mFilters.get(filterKey).updateFilterValue(value);
        } catch (NullPointerException e) {
            throw new InvalidFilterException();
        } // UnsupportedOperationException is passed through
    }

    public int getFilterValue(String filterKey) {
        try {
            return mFilters.get(filterKey).getFilterValue();
        } catch (NullPointerException e) {
            throw new InvalidFilterException();
        } // UnsupportedOperationException is passed through
    }

    /**
     * Loads the previously saved filters.
     */
    private void load() {
        for (Map.Entry<String, UserFilter> entry : mFilters.entrySet()) {
            final String name = entry.getKey();
            final UserFilter filter = entry.getValue();

            filter.setActive(mSettingsRepository.isUserFilterActive(name));
            try {
                final int defaultValue = filter.getFilterValue();
                filter.updateFilterValue(
                        mSettingsRepository.getUserFilterValue(name, defaultValue));
            } catch (UnsupportedOperationException e) {
                // Ignore.
            }
        }
    }

    /**
     * Persists the filters and their values s.t. they can be restored when the app is restarted the
     * next time.
     */
    public void save() {
        for (Map.Entry<String, UserFilter> entry : mFilters.entrySet()) {
            final String name = entry.getKey();
            final UserFilter filter = entry.getValue();

            mSettingsRepository.setUserFilterActive(name, filter.isActive);
            try {
                mSettingsRepository.setUserFilterValue(name, filter.getFilterValue());
            } catch (UnsupportedOperationException e) {
                // Ignore.
            }
        }
    }

    abstract class UserFilter {
        boolean isActive = false;

        void setActive(boolean isActive) {
            this.isActive = isActive;
        }
        boolean isActive() {
            return isActive;
        }
        void updateFilterValue(int value) {
          throw new UnsupportedOperationException();
        }
        int getFilterValue() {
          throw new UnsupportedOperationException();
        }

        boolean filterUser(SimpleUser user) {
            if (!isActive) {
                return true;
            }
            return filterUserInternal(user);
        }
        abstract boolean filterUserInternal(SimpleUser user);
    }

    // A filter that returns true iff the user is a favaorite user.
    private class FavoriteUserFilter extends UserFilter {
        @Override
        boolean filterUserInternal(SimpleUser user) {
            return mFavoriteRepository.isFavorite(user.id);
        }
    }

    // A filter that returns true iff the user is currently available.
    private class CurrentlyAvailableFilter extends UserFilter {
        @Override
        boolean filterUserInternal(SimpleUser user) {
            return user.isCurrentlyAvailable;
        }
    }

    // A filter that returns true iff the user has accessed their account in the past N (defaults to
    // \infty) days.
    private class RecentActivityFilter extends UserFilter {
        private int mLastAccessThresholdInDays = -1;
        @Override
        boolean filterUserInternal(SimpleUser user) {
            if (mLastAccessThresholdInDays < 0) {
                return true;
            }
            return TimeUnit.DAYS.convert(System.currentTimeMillis() -
                user.lastAccess.getTime(), TimeUnit.MILLISECONDS) < mLastAccessThresholdInDays;
        }

        @Override
        void updateFilterValue(int value) {
            mLastAccessThresholdInDays = value;
        }

        @Override
        int getFilterValue() {
            return mLastAccessThresholdInDays;
        }

        @Override
        boolean isActive() {
            return isActive && mLastAccessThresholdInDays >= 0;
        }
    }
}
