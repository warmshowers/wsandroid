package fi.bitrite.android.ws.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

abstract class Repository<T> {
    enum Freshness {
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
         *
         * This is initialized as REFRESHING to avoid a race in #get().
         */
        Freshness freshness = Freshness.REFRESHING;
    }

    private final BehaviorSubject<List<Observable<Resource<T>>>> mAllObservable =
            BehaviorSubject.create();
    private final ConcurrentMap<Integer, CacheEntry> mCache = new ConcurrentHashMap<>();

    Completable save(int id, @NonNull T data) {
        // Does the save in the background.
        return Completable.create(emitter -> {
            CacheEntry newCacheEntry = new CacheEntry();

            CacheEntry cacheEntry = mCache.putIfAbsent(id, newCacheEntry);
            if (cacheEntry == null) {
                cacheEntry = newCacheEntry;
            }

            cacheEntry.data.onNext(Resource.success(data));

            saveInDb(id, data);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io());
    }
    abstract void saveInDb(int id, @NonNull T data);

    /**
     * Returns an observable that is fired every time an entry is added to or removed from the cache.
     */
    Observable<List<Observable<Resource<T>>>> getAll() {
        return mAllObservable;
    }

    protected Set<Integer> getAllIdsRaw() {
        return mCache.keySet();
    }

    /**
     * Gets an element from the cache. If it not yet there it will be loaded from the db and a
     * refresh from the network is started. A network sync is also started if its freshness is OLD.
     */
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

            // We notify the all-observable about the change in the set of loaded threads.
            notifyAllChanged();
        }

        Resource<T> resource = cacheEntry.data.getValue();
        if (isNewCacheEntry || cacheEntry.freshness == Freshness.OLD ||
                resource == null || resource.isError()) {
            // We load it from the network.

            Observable<LoadResult<T>> lfn = loadFromNetwork(id);
            if (lfn != null) {
                cacheEntry.freshness = Freshness.REFRESHING;
                observables.add(lfn.subscribeOn(Schedulers.io()));
            }
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

                        if (observables.size() == 2) {
                            // There is an ongoing network load.
                            Resource<T> currentData = cacheData.getValue();
                            if (currentData == null || currentData.isSuccess()) {
                                // The above check avoids to set the loading value in the rare case
                                // where the network response is received earlier than the one from
                                // the db.
                                // TODO(saemy): Can this even happen in Observable.concat?
                                cacheData.onNext(Resource.loading(data));
                            }
                        } else {
                            // The element does not get refreshed from the network.
                            cacheData.onNext(Resource.success(data));
                        }
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

    /**
     * Returns the cache entry or null if none is there.
     */
    @Nullable
    T getRaw(int id) {
        CacheEntry cacheEntry = mCache.get(id);
        Resource<T> resource = cacheEntry != null ? cacheEntry.data.getValue() : null;
        return resource != null ? resource.data : null;
    }

    /**
     * Evicts the given entry from the cache and starts a reload.
     */
    Observable<Resource<T>> reload(int id, ShouldSaveInDb shouldSaveInDb) {
        return reload(id, getRaw(id), shouldSaveInDb);
    }
    Observable<Resource<T>> reload(int id, T currentValue, ShouldSaveInDb shouldSaveInDb) {
        put(id, Resource.loading(currentValue), Freshness.OLD);
        return get(id, shouldSaveInDb);
    }

    /**
     * Puts the given element into the cache.
     */
    void put(int id, Resource<T> resource, Freshness freshness) {
        CacheEntry defaultCacheEntry = new CacheEntry();
        defaultCacheEntry.freshness = freshness;

        CacheEntry cacheEntry = mCache.putIfAbsent(id, defaultCacheEntry);
        if (cacheEntry == null) {
            cacheEntry = defaultCacheEntry;

            // We notify the all-observable about the change in the set of loaded threads.
            notifyAllChanged();
        } else {
            cacheEntry.freshness = freshness;
        }

        cacheEntry.data.onNext(resource);
    }

    // Removes the given ids from the cache.
    void pop(List<Integer> ids) {
        for (Integer id : ids) {
            mCache.remove(id);
        }
        notifyAllChanged();
    }
    void popExcept(List<Integer> ids) {
        for (Integer entryId : mCache.keySet()) {
           if (!ids.contains(entryId)) {
               mCache.remove(entryId);
           }
        }
        notifyAllChanged();
    }

    void markAsOld(int id) {
        CacheEntry cacheEntry = mCache.get(id);
        if (cacheEntry != null) {
            cacheEntry.freshness = Freshness.OLD;
        }
    }

    private void notifyAllChanged() {
        List<Observable<Resource<T>>> all = new ArrayList<>(mCache.size());
        for (CacheEntry cacheEntry : mCache.values()) {
            all.add(cacheEntry.data);
        }
        mAllObservable.onNext(all);
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
