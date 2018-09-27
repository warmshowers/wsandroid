package fi.bitrite.android.ws.util;

import org.osmdroid.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.api.response.UserSearchByLocationResponse;
import fi.bitrite.android.ws.di.AppScope;
import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.repository.UserRepository;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Loads and caches users by region. If a region is queried twice, the cached users are returned.
 * This is also true for overlapping areas where only the ones which are not yet covered are
 * fetched.
 */
@AccountScope
public class UserRegionalCache {
    @Inject AppScopeUserRegionalCache mAppScopeUserRegionalCache;
    @Inject UserRepository mUserRepository;

    @Inject
    UserRegionalCache() {
    }

    public Collection<UserSearchByLocationResponse.User> getAllCached() {
        return mAppScopeUserRegionalCache.mUserCache;
    }

    public Observable<List<UserSearchByLocationResponse.User>> searchByLocation(
            BoundingBox boundingBox) {
        return mAppScopeUserRegionalCache.searchByLocation(boundingBox, mUserRepository);
    }

    @AppScope
    static class AppScopeUserRegionalCache {
        private final LoadedArea mLoadedArea = new LoadedArea();
        private final Collection<UserSearchByLocationResponse.User> mUserCache = new HashSet<>();

        @Inject
        AppScopeUserRegionalCache() {
        }

        Observable<List<UserSearchByLocationResponse.User>> searchByLocation(
                BoundingBox boundingBox, UserRepository userRepository) {
            return Observable.<List<UserSearchByLocationResponse.User>>create(emitter -> {
                List<BoundingBox> unloadedAreas = mLoadedArea.subtractLoadedAreas(boundingBox);
                List<Observable<List<UserSearchByLocationResponse.User>>> observables =
                        new ArrayList<>(unloadedAreas.size());
                List<BoundingBox> successfullyLoadedAreas = new ArrayList<>(unloadedAreas.size());
                for (BoundingBox unloadedArea : unloadedAreas) {
                    observables.add(userRepository.searchByLocation(unloadedArea)
                            .map(users -> {
                                successfullyLoadedAreas.add(unloadedArea);
                                mUserCache.addAll(users);
                                return users;
                            }));
                }

                Observable.mergeDelayError(observables)
                        .doOnComplete(() -> {
                            // We add one big (possibly overlapping) rather than several small
                            // unloaded areas.
                            if (!unloadedAreas.isEmpty()) {
                                mLoadedArea.addLoadedArea(boundingBox);
                            }
                        })
                        .doOnError(e -> {
                            // Some areas failed to load -> only put the successful ones.
                            for (BoundingBox loadedArea : successfullyLoadedAreas) {
                                mLoadedArea.addLoadedArea(loadedArea);
                            }
                        })
                        .subscribe(emitter::onNext, emitter::onError, emitter::onComplete);
            }).subscribeOn(Schedulers.computation());
        }
    }
}
