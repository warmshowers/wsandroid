package fi.bitrite.android.ws.repository;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.api_new.WarmshowersService;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.UserDao;
import io.reactivex.Maybe;
import io.reactivex.Observable;

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

    // Exposes it public.
    @Override
    public Observable<Resource<Host>> get(int userId, ShouldSaveInDb shouldSaveInDb) {
        return super.get(userId, shouldSaveInDb);
    }

    public void save(@NonNull Host user) {
        save(user.getId(), user);
    }

    @Override
    void saveInDb(int id, @NonNull Host user) {
        mUserDao.save(user);
    }
    public Observable<Resource<Host>> getUser(int userId) {
        return get(userId);
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
        return mWarmshowersService.fetchUser(userId).map(apiUserResponse -> {
            if (apiUserResponse.isSuccessful()) {
                return new LoadResult<>(
                        LoadResult.Source.NETWORK, apiUserResponse.body().toHost());
            } else {
                throw new Error(apiUserResponse.errorBody().toString());
            }
        });
    }
}
