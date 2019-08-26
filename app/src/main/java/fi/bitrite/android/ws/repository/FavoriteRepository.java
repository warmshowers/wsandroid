package fi.bitrite.android.ws.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.persistence.FavoriteDao;
import io.reactivex.Observable;

@AccountScope
public class FavoriteRepository {
    @Inject FavoriteDao mFavoriteDao;
    @Inject UserRepository mUserRepository;

    @Inject
    FavoriteRepository() {
    }

    public List<Observable<Resource<User>>> getFavorites() {
        List<Integer> userIds = mFavoriteDao.loadAll();

        List<Observable<Resource<User>>> favorites = new ArrayList<>(userIds.size());
        for (Integer userId : userIds) {
            favorites.add(mUserRepository.get(userId));
        }

        return favorites;
    }

    public boolean isFavorite(int userId) {
        return mFavoriteDao.exists(userId);
    }

    public void add(@NonNull User user, @Nullable List<Feedback> feedbacks) {
        mFavoriteDao.add(user, feedbacks);
    }

    public void remove(int userId) {
        mFavoriteDao.remove(userId);
    }
}
