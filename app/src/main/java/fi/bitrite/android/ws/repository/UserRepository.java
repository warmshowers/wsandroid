package fi.bitrite.android.ws.repository;

import org.osmdroid.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import fi.bitrite.android.ws.api.WarmshowersAccountWebservice;
import fi.bitrite.android.ws.api.model.ApiUser;
import fi.bitrite.android.ws.api.response.UserSearchByLocationResponse;
import fi.bitrite.android.ws.di.AppScope;
import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.persistence.UserDao;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

/**
 * This repository is split into two parts. One lives in the account scope as we need access to its
 * webservice. However, users are cached in the app-level as they stay the same and are not private.
 */
@AccountScope
public class UserRepository {
    @Inject AppUserRepository _mAppUserRepository;
    @Inject WarmshowersAccountWebservice mWebservice;

    @Inject
    UserRepository() {
    }

    public Observable<Resource<User>> get(int userId) {
        return getAppUserRepository().get(userId, Repository.ShouldSaveInDb.IF_ALREADY_IN_DB);
    }
    @NonNull
    public List<Observable<Resource<User>>> get(@NonNull Collection<Integer> userIds) {
        return get(userIds, Repository.ShouldSaveInDb.IF_ALREADY_IN_DB);
    }
    public List<Observable<Resource<User>>> get(@NonNull Collection<Integer> userIds,
                                                Repository.ShouldSaveInDb shouldSaveInDb) {
        return getAppUserRepository().get(userIds, shouldSaveInDb);
    }

    // Exposes it public.
    public Observable<Resource<User>> get(int userId, Repository.ShouldSaveInDb shouldSaveInDb) {
        return getAppUserRepository().get(userId, shouldSaveInDb);
    }

    public Completable save(@NonNull User user) {
        return getAppUserRepository().save(user);
    }

    public Observable<List<Integer>> searchByKeyword(String keyword) {
        return getAppUserRepository().searchByKeyword(keyword);
    }

    public Observable<List<UserSearchByLocationResponse.User>> searchByLocation(
            BoundingBox boundingBox) {
        return getAppUserRepository().searchByLocation(boundingBox);
    }
    public void markAsOld(int id) {
        getAppUserRepository().markAsOld(id);
    }

    /**
     * Sets the last webservice in the app-scoped User repository and returns it.
     * Always use this function instead of the direct access to the variable!
     */
    private AppUserRepository getAppUserRepository() {
        _mAppUserRepository.mLastWebservice = mWebservice;
        return _mAppUserRepository;
    }

    /**
     * This is the app scope instance that is shared amongst all the account-scope instances of the
     * {@link UserRepository}. We share it to only have one cache. However, we need a
     * {@link WarmshowersAccountWebservice} instance, which lives in the account scope, to fetch
     * users.
     */
    @AppScope
    public static class AppUserRepository extends Repository<User> {
        @Inject UserDao mUserDao;
        WarmshowersAccountWebservice mLastWebservice;

        @Inject
        AppUserRepository() {
        }

        List<Observable<Resource<User>>> get(@NonNull Collection<Integer> userIds,
                                             ShouldSaveInDb shouldSaveInDb) {
            List<Observable<Resource<User>>> users = new ArrayList<>(userIds.size());
            for (Integer userId : userIds) {
                users.add(get(userId, shouldSaveInDb));
            }
            return users;
        }

        public Completable save(@NonNull User user) {
            return saveRx(user.id, user);
        }

        @Override
        void saveInDb(int id, @NonNull User user) {
            mUserDao.save(user);
        }

        @Override
        Observable<LoadResult<User>> loadFromDb(int userId) {
            return Maybe.<LoadResult<User>>create(emitter -> {
                User user = mUserDao.load(userId);
                if (user != null) {
                    emitter.onSuccess(new LoadResult<>(LoadResult.Source.DB, user));
                } else {
                    emitter.onComplete();
                }
            }).toObservable();
        }

        @Override
        Observable<LoadResult<User>> loadFromNetwork(int userId) {
            return mLastWebservice.fetchUser(userId)
                    .subscribeOn(Schedulers.io())
                    .map(apiUserResponse -> {
                        if (!apiUserResponse.isSuccessful()) {
                            throw new Error(apiUserResponse.errorBody().string());
                        }

                        return new LoadResult<>(
                                LoadResult.Source.NETWORK, apiUserResponse.body().toUser());
                    });
        }


        Observable<List<Integer>> searchByKeyword(String keyword) {
            // TODO(saemy): Change the webservice s.t. it returns a total number of results to be
            //              able to do paging.
            return mLastWebservice.searchUsersByKeyword(keyword, 0, 100)
                    .subscribeOn(Schedulers.io())
                    .map(apiResponse -> {
                        if (!apiResponse.isSuccessful()) {
                            throw new Error(apiResponse.errorBody().string());
                        }

                        Collection<ApiUser> apiUsers = apiResponse.body().users.values();
                        List<Integer> userIds = new ArrayList<>(apiUsers.size());
                        beginPutMany();
                        try {
                            for (ApiUser apiUser : apiUsers) {
                                put(apiUser.id, Resource.success(apiUser.toUser()),
                                        Freshness.FRESH);
                                userIds.add(apiUser.id);
                            }
                        } finally {
                            endPutMany();
                        }

                        return userIds;
                    });
        }

        Observable<List<UserSearchByLocationResponse.User>> searchByLocation(
                BoundingBox boundingBox) {

            final double minLat = boundingBox.getLatSouth();
            final double minLon = boundingBox.getLonWest();
            final double maxLat = boundingBox.getLatNorth();
            final double maxLon = boundingBox.getLonEast();
            final double centerLat = (minLat + maxLat) / 2.0f;
            final double centerLon = (minLon + maxLon) / 2.0f;

            return mLastWebservice.searchUsersByLocation(minLat, minLon, maxLat, maxLon, centerLat,
                    centerLon, WarmshowersAccountWebservice.SEARCH_USER_DEFAULT_LIMIT)
                    .subscribeOn(Schedulers.io())
                    .map(apiResponse -> {
                        if (!apiResponse.isSuccessful()) {
                            throw new HttpException(apiResponse);
                        }

                        return apiResponse.body().users;
                    });
        }
    }
}
