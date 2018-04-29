package fi.bitrite.android.ws.ui.model;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import io.reactivex.subjects.BehaviorSubject;

/**
 * Represents the data in a row of the navigation drawer.
 */
public class NavigationItem {

    public final String tag;
    @DrawableRes public final int iconResourceId;
    @StringRes public final int labelResourceId;
    public final BehaviorSubject<Integer> notificationCount = BehaviorSubject.createDefault(0);

    public NavigationItem(String tag, @DrawableRes int iconResourceId,
                          @StringRes int labelResourceId) {
        this.tag = tag;
        this.iconResourceId = iconResourceId;
        this.labelResourceId = labelResourceId;
    }
}
