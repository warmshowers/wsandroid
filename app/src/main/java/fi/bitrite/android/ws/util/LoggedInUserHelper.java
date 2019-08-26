package fi.bitrite.android.ws.util;

import android.accounts.Account;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import javax.inject.Inject;

import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.Repository;
import fi.bitrite.android.ws.repository.UserRepository;
import io.reactivex.subjects.BehaviorSubject;

@AccountScope
public class LoggedInUserHelper {
    private final int mUserId;
    private final BehaviorSubject<MaybeNull<User>> mLoggedInUser =
            BehaviorSubject.createDefault(new MaybeNull<>());

    @Inject
    public LoggedInUserHelper(Account account,
                              AccountManager accountManager,
                              UserRepository userRepository) {
        mUserId = accountManager.getUserId(account);
        assert mUserId != AccountManager.UNKNOWN_USER_ID;

        userRepository.get(mUserId, Repository.ShouldSaveInDb.YES)
                .subscribe(resource -> {
                    if (resource.isError() && resource.error != null) {
                        Log.e(LoggedInUserHelper.class.getName(),
                                resource.error.getMessage());
                    }

                    User user = resource.data;
                    if (user != null && !user.equals(mLoggedInUser.getValue().data)) {
                        mLoggedInUser.onNext(new MaybeNull<>(user));
                    }
                }); // FIXME(saemy): Error handling.
    }

    public int getId() {
        return mUserId;
    }

    @Nullable
    public User get() {
        return mLoggedInUser.getValue().data;
    }

    @NonNull
    public BehaviorSubject<MaybeNull<User>> getRx() {
        return mLoggedInUser;
    }
}
