package fi.bitrite.android.ws.ui.util;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.repository.FavoriteRepository;


/**
 * Manages filters for hosts displayed in MapFragment.
 */
@AccountScope
public class HostFilterManager {
    public static final String RECENT_ACTIVITY_FILTER_KEY = "recentActivityFilter";
    public static final String CURRENTLY_AVAILABLE_FILTER_KEY = "currentlyAvailableFilter";
    public static final String FAVORITE_HOST_FILTER_KEY = "favoriteHost";

    private final HashMap<String, HostFilter> mFilters = new HashMap();

    @Inject FavoriteRepository mFavoriteRepository;

    @Inject
    HostFilterManager() {
        mFilters.put(RECENT_ACTIVITY_FILTER_KEY, new RecentActivityFilter());
        mFilters.put(CURRENTLY_AVAILABLE_FILTER_KEY, new CurrentlyAvailableFilter());
        mFilters.put(FAVORITE_HOST_FILTER_KEY, new FavoriteHostFilter());
    }

    public boolean filterHost(SimpleUser host) {
        for (HostFilter filter : mFilters.values()) {
            if (!filter.filterHost(host)) {
                return false;
            }
        }
        return true;
    }

    public boolean isAnyFilterActive() {
        for (HostFilter filter : mFilters.values()) {
            if (filter.isActive()) {
                return true;
            }
        }
        return false;
    }

    public class InvalidFilterException extends RuntimeException { }

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

    abstract class HostFilter {
        protected boolean isActive = false;

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

        boolean filterHost(SimpleUser host) {
            if (!isActive) {
                return true;
            }
            return filterHostInternal(host);
        }
        abstract boolean filterHostInternal(SimpleUser host);
    }

    // A filter that returns true iff the host is a favaorite host.
    private class FavoriteHostFilter extends HostFilter {
        @Override
        boolean filterHostInternal(SimpleUser host) {
            return mFavoriteRepository.isFavorite(host.id);
        }
    }

    // A filter that returns true iff the host is currently available.
    private class CurrentlyAvailableFilter extends HostFilter {
        @Override
        boolean filterHostInternal(SimpleUser host) {
            return host.isCurrentlyAvailable;
        }
    }

    // A filter that returns true iff the host has accessed their account in the past N (defaults to
    // \infty) days.
    private class RecentActivityFilter extends HostFilter {
        private int mLastAccessThresholdInDays = -1;
        @Override
        boolean filterHostInternal(SimpleUser host) {
            if (mLastAccessThresholdInDays < 0) {
                return true;
            }
            return TimeUnit.DAYS.convert(System.currentTimeMillis() -
                host.lastAccess.getTime(), TimeUnit.MILLISECONDS) < mLastAccessThresholdInDays;
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
