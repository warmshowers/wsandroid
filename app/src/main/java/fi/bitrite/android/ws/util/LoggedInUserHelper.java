package fi.bitrite.android.ws.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.repository.UserRepository;
import io.reactivex.subjects.BehaviorSubject;

@Singleton
public class LoggedInUserHelper {
    private final BehaviorSubject<MaybeNull<Host>> mLoggedInUser =
            BehaviorSubject.createDefault(new MaybeNull<>());

    @Inject
    public LoggedInUserHelper(AccountManager accountManager, UserRepository userRepository) {
        accountManager.getCurrentUserId().subscribe(userId -> {
            if (userId != AccountManager.UNKNOWN_USER_ID) {
                userRepository.get(userId, UserRepository.ShouldSaveInDb.YES).subscribe(resource -> {
                    Host user = resource.data;
                    if  (user == null) {
                        Log.e(LoggedInUserHelper.class.getName(), resource.error.getMessage());
                        return;
                    }

                    mLoggedInUser.onNext(new MaybeNull<>(user));
                }); // FIXME(saemy): Error handling.
            } else {
                mLoggedInUser.onNext(new MaybeNull<>());
            }
        });
    }

    @Nullable
    public Host get() {
        return mLoggedInUser.getValue().data;
    }

    public int getId() {
        Host user = get();
        return user != null ? user.getId() : -1;
    }

    @NonNull
    public BehaviorSubject<MaybeNull<Host>> getRx() {
        return mLoggedInUser;
    }
}
