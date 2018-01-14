package fi.bitrite.android.ws.repository;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

abstract class Repository<T> {
    private enum Freshness {
        OLD,
        REFRESHING,
        FRESH,
    };

    public enum ShouldSaveInDb {
        YES,
        NO,
        IF_ALREADY_IN_DB
    };

    class CacheEntry {
        final BehaviorSubject<Resource<T>> data = BehaviorSubject.create();

        /**
         * Whether the user is loaded from the Warmshowers service or from the local db only.
         */
        Freshness freshness = Freshness.OLD;
    }

    private final ConcurrentMap<Integer, CacheEntry> mCache = new ConcurrentHashMap<>();

    void save(int id, @NonNull T data) {
        CacheEntry newCacheEntry = new CacheEntry();

        CacheEntry cacheEntry = mCache.putIfAbsent(id, newCacheEntry);
        if (cacheEntry == null) {
            cacheEntry = newCacheEntry;
        }

        saveInDb(id, data);
        cacheEntry.data.onNext(Resource.success(data));
    }
    abstract void saveInDb(int id, @NonNull T data);

    Observable<Resource<T>> get(int id, ShouldSaveInDb shouldSaveInDb) {
        CacheEntry defaultCacheEntry = new CacheEntry();

        CacheEntry cacheEntry = mCache.putIfAbsent(id, defaultCacheEntry);
        boolean isNewCacheEntry = cacheEntry == null;

        List<Observable<LoadResult<T>>> observables = new ArrayList<>();
        if (isNewCacheEntry) {
            // There was no entry in the cache -> we must load it.
            cacheEntry = defaultCacheEntry;

            // We load it from the db.
            observables.add(loadFromDb(id).subscribeOn(Schedulers.io()));
        }

        if (isNewCacheEntry || cacheEntry.freshness == Freshness.OLD ||
                cacheEntry.data.getValue().isError()) {
            // We load it from the network.
            cacheEntry.freshness = Freshness.REFRESHING;
            observables.add(loadFromNetwork(id).subscribeOn(Schedulers.io()));
        }

        if (!observables.isEmpty()) {
            final CacheEntry ce = cacheEntry;
            final BehaviorSubject<Resource<T>> cacheData = cacheEntry.data;
            final AtomicBoolean isInDb = new AtomicBoolean(false);

            // We listen for the results.
            Observable.concat(observables).subscribe(result -> {
                T data = result.data;

                if (result.isFromDb()) {
                    // We got the data from the db. We set it as an intermediate and wait until we
                    // get the truth from the network.
                    if (data != null) {
                        isInDb.set(true);
                        cacheData.onNext(Resource.loading(data));
                    }
                } else {
                    // We got the data from the network.
                    boolean shouldSave =
                            shouldSaveInDb == ShouldSaveInDb.YES ||
                            (isInDb.get() && shouldSaveInDb == ShouldSaveInDb.IF_ALREADY_IN_DB);
                    if (shouldSave) {
                        // We save it to the db.
                        saveInDb(id, result.data);
                    }
                    ce.freshness = Freshness.FRESH;

                    // We populate the data.
                    cacheData.onNext(Resource.success(result.data));
                }
            }, throwable -> {
                Resource<T> res = cacheData.getValue();
                cacheData.onNext(Resource.error(throwable, res == null ? null : res.data));
            });
        }

        return cacheEntry.data;
    }

    abstract Observable<LoadResult<T>> loadFromDb(int id);
    abstract Observable<LoadResult<T>> loadFromNetwork(int id);

    static class LoadResult<T> {
        enum Source {
            DB,
            NETWORK,
        };

        final Source source;
        final T data;

        LoadResult(Source source, T data) {
            this.source = source;
            this.data = data;
        }

        boolean isFromDb() {
            return source == Source.DB;
        }
        boolean isFromNetwork() {
            return source == Source.NETWORK;
        }
    }
}
