package fi.bitrite.android.ws.activity;

import android.app.Activity;
import android.support.annotation.NonNull;

import io.reactivex.subjects.BehaviorSubject;

public class ActivityHelper {

    private final static Object NULL_OBJECT = new Object();
    private BehaviorSubject<Object> mCurrentActivity = BehaviorSubject.createDefault(NULL_OBJECT);

    public void setCurrentActivity(Activity activity) {
        mCurrentActivity.onNext(activity == null ? NULL_OBJECT : activity);
    }

    public Activity getCurrentActivity() {
        Object current = mCurrentActivity.getValue();
        return current == NULL_OBJECT
                ? null
                : (Activity) current;
    }

    @NonNull
    public Activity waitForAvailableActivity() {
        while (mCurrentActivity.getValue() == NULL_OBJECT) {
            mCurrentActivity.blockingNext();
        }

        return getCurrentActivity();
    }
}
