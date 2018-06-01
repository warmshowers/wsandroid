package fi.bitrite.android.ws.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.FavoriteDao;
import io.reactivex.Observable;

@AccountScope
public class FavoriteRepository {
    @Inject FavoriteDao mFavoriteDao;
    @Inject UserRepository mUserRepository;

    @Inject
    FavoriteRepository() {
    }

    public List<Observable<Resource<Host>>> getFavorites() {
        List<Integer> userIds = mFavoriteDao.loadAll();

        List<Observable<Resource<Host>>> favorites = new ArrayList<>(userIds.size());
        for (Integer userId : userIds) {
            favorites.add(mUserRepository.get(userId));
        }

        return favorites;
    }

    public boolean isFavorite(int userId) {
        return mFavoriteDao.exists(userId);
    }

    public void add(@NonNull Host user, @Nullable List<Feedback> feedbacks) {
        mFavoriteDao.add(user, feedbacks);
    }

    public void remove(int userId) {
        mFavoriteDao.remove(userId);
    }
}
