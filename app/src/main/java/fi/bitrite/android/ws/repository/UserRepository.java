package fi.bitrite.android.ws.repository;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.api_new.WarmshowersService;
import fi.bitrite.android.ws.api_new.response.UserSearchByLocationResponse;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.UserDao;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

@Singleton
public class UserRepository extends Repository<Host> {

    @Inject UserDao mUserDao;
    @Inject WarmshowersService mWarmshowersService;

    @Inject
    UserRepository() {
    }

    public Observable<Resource<Host>> get(int userId) {
        return get(userId, ShouldSaveInDb.IF_ALREADY_IN_DB);
    }
    @NonNull
    public List<Observable<Resource<Host>>> get(@NonNull Collection<Integer> userIds) {
        return get(userIds, ShouldSaveInDb.IF_ALREADY_IN_DB);
    }
    public List<Observable<Resource<Host>>> get(@NonNull Collection<Integer> userIds,
                                                ShouldSaveInDb shouldSaveInDb) {
        List<Observable<Resource<Host>>> users = new ArrayList<>(userIds.size());
        for (Integer userId : userIds) {
            users.add(get(userId, shouldSaveInDb));
        }
        return users;
    }

    // Exposes it public.
    @Override
    public Observable<Resource<Host>> get(int userId, ShouldSaveInDb shouldSaveInDb) {
        return super.get(userId, shouldSaveInDb);
    }

    public Completable save(@NonNull Host user) {
        return save(user.getId(), user);
    }

    @Override
    void saveInDb(int id, @NonNull Host user) {
        mUserDao.save(user);
    }

    @Override
    Observable<LoadResult<Host>> loadFromDb(int userId) {
        return Maybe.<LoadResult<Host>>create(emitter -> {
            Host user = mUserDao.load(userId);
            if (user != null) {
                emitter.onSuccess(new LoadResult<>(LoadResult.Source.DB, user));
            } else {
                emitter.onComplete();
            }
        }).toObservable();
    }

    @Override
    Observable<LoadResult<Host>> loadFromNetwork(int userId) {
        return mWarmshowersService.fetchUser(userId)
                .subscribeOn(Schedulers.io())
                .map(apiUserResponse -> {
                    if (apiUserResponse.isSuccessful()) {
                        return new LoadResult<>(
                                LoadResult.Source.NETWORK, apiUserResponse.body().toHost());
                    } else {
                        throw new Error(apiUserResponse.errorBody().toString());
                    }
                });
    }


    public Observable<List<UserSearchByLocationResponse.User>> searchByLocation(
            LatLng northEast, LatLng southWest) {

        final double minLat = southWest.latitude;
        final double minLon = southWest.longitude;
        final double maxLat = northEast.latitude;
        final double maxLon = northEast.longitude;
        final double centerLat = (minLat + maxLat) / 2.0f;
        final double centerLon = (minLon + maxLon) / 2.0f;

        return mWarmshowersService.searchUsersByLocation(minLat, minLon, maxLat, maxLon, centerLat,
                centerLon, WarmshowersService.SEARCH_USER_DEFAULT_LIMIT)
                .subscribeOn(Schedulers.io())
                .map(apiResponse -> {
                    if (apiResponse.isSuccessful()) {
                        return apiResponse.body().users;
                    } else {
                        throw new Error(apiResponse.errorBody().toString());
                    }
                });
    }
}
