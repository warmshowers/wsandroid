package fi.bitrite.android.ws.util;

import android.accounts.Account;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import javax.inject.Inject;

import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.repository.Repository;
import fi.bitrite.android.ws.repository.UserRepository;
import io.reactivex.subjects.BehaviorSubject;

@AccountScope
public class LoggedInUserHelper {
    private final int mUserId;
    private final BehaviorSubject<MaybeNull<Host>> mLoggedInUser =
            BehaviorSubject.createDefault(new MaybeNull<>());

    @Inject
    public LoggedInUserHelper(Account account,
                              AccountManager accountManager,
                              UserRepository userRepository) {
        mUserId = accountManager.getUserId(account);
        assert mUserId != AccountManager.UNKNOWN_USER_ID;

        userRepository.get(mUserId, Repository.ShouldSaveInDb.YES)
                .subscribe(resource -> {
                    Host user = resource.data;
                    if (user == null) {
                        Log.e(LoggedInUserHelper.class.getName(),
                                resource.error.getMessage());
                        return;
                    }

                    mLoggedInUser.onNext(new MaybeNull<>(user));
                }); // FIXME(saemy): Error handling.
    }

    public int getId() {
        return mUserId;
    }

    @Nullable
    public Host get() {
        return mLoggedInUser.getValue().data;
    }

    @NonNull
    public BehaviorSubject<MaybeNull<Host>> getRx() {
        return mLoggedInUser;
    }
}
