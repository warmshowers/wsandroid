package fi.bitrite.android.ws.ui.util;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.AppScope;
import io.reactivex.subjects.BehaviorSubject;

@AppScope
public class ActionBarTitleHelper {
    private final BehaviorSubject<CharSequence> mTitle = BehaviorSubject.create();

    @Inject
    public ActionBarTitleHelper() {
    }

    public BehaviorSubject<CharSequence> get() {
        return mTitle;
    }

    public void set(CharSequence title) {
        mTitle.onNext(title);
    }
}
