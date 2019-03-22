package fi.bitrite.android.ws.ui.util;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.repository.FavoriteRepository;


/**
 * Manages filters for users displayed in MapFragment.
 */
@AccountScope
public class UserFilterManager {
    public static final String RECENT_ACTIVITY_FILTER_KEY = "recentActivityFilter";
    public static final String CURRENTLY_AVAILABLE_FILTER_KEY = "currentlyAvailableFilter";
    public static final String FAVORITE_USER_FILTER_KEY = "favoriteUser";

    private final HashMap<String, UserFilter> mFilters = new HashMap<>();

    @Inject FavoriteRepository mFavoriteRepository;

    @Inject
    UserFilterManager() {
        mFilters.put(RECENT_ACTIVITY_FILTER_KEY, new RecentActivityFilter());
        mFilters.put(CURRENTLY_AVAILABLE_FILTER_KEY, new CurrentlyAvailableFilter());
        mFilters.put(FAVORITE_USER_FILTER_KEY, new FavoriteUserFilter());
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
        if (activated) {
            activateFilter(filterKey);
        } else {
            deactivateFilter(filterKey);
        }
    }

    public void activateFilter(String filterKey) {
        try {
            mFilters.get(filterKey).activateFilter();
        } catch (NullPointerException e) {
            throw new InvalidFilterException();
        }
    }

    public void deactivateFilter(String filterKey) {
        try {
            mFilters.get(filterKey).deactivateFilter();
        } catch (NullPointerException e) {
            throw new InvalidFilterException();
        }
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

    abstract class UserFilter {
        boolean isActive = false;

        void activateFilter() {
            isActive = true;
        }
        void deactivateFilter() {
            isActive = false;
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
