package fi.bitrite.android.ws.ui.listadapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.ui.model.NavigationItem;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class NavigationListAdapter extends ArrayAdapter<NavigationListAdapter.Item> {
    private final Observable<NavigationItem> mOnClickSubject;

    public static NavigationListAdapter create(
            Context context, List<NavigationItem> navigationItems,
            BehaviorSubject<String> currentTag) {

        Subject<NavigationItem> onClickSubject = PublishSubject.create();

        List<Item> items = new ArrayList<>(navigationItems.size());
        for (NavigationItem navigationItem : navigationItems) {
            items.add(new Item(navigationItem, currentTag, onClickSubject));
        }

        return new NavigationListAdapter(context, items, onClickSubject);
    }

    private NavigationListAdapter(Context context, List<Item> items,
                                  Observable<NavigationItem> onClickSubject) {
        super(context, R.layout.item_navigation, items);
        mOnClickSubject = onClickSubject;
    }

    public Observable<NavigationItem> getOnClickSubject() {
        return mOnClickSubject;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Item item = getItem(position);

        // We need a new view for every item since we want to be able to change its background if
        // selected.
        if (item.hasView()) {
            return item.getView();
        } else {
            View view = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_navigation, parent, false);
            item.setView(view);
            return view;
        }
    }

    static class Item {
        private final NavigationItem mNavigationItem;
        private final BehaviorSubject<String> mCurrentTag;
        private final Subject<NavigationItem> mOnClickSubject;

        private Disposable mCurrentTagDisposable;

        private View mView;

        @BindColor(R.color.backgroundLightGrey) int mColorActive;
        @BindColor(android.R.color.transparent) int mColorInactive;

        @BindView(R.id.icon_menu_item) ImageView mIcon;
        @BindView(R.id.lbl_menu_item) TextView mLabel;
        @BindView(R.id.navigation_item_layout) LinearLayout mNavigationItemLayout;

        Item(NavigationItem navigationItem, BehaviorSubject<String> currentTag,
             Subject<NavigationItem> onClickSubject) {
            mNavigationItem = navigationItem;
            mCurrentTag = currentTag;
            mOnClickSubject = onClickSubject;
        }

        @OnClick(R.id.navigation_item_layout)
        public void onClick() {
            // Informs the observers about this click event.
            mOnClickSubject.onNext(mNavigationItem);
        }

        void setView(View view) {
            mView = view;
            ButterKnife.bind(this, view);
            mIcon.setImageResource(mNavigationItem.iconResourceId);
            mLabel.setText(mNavigationItem.labelResourceId);

            // Disconnect from old notifications.
            if (mCurrentTagDisposable != null) {
                mCurrentTagDisposable.dispose();
            }

            mCurrentTagDisposable = mCurrentTag.subscribe(currentItemTag -> {
                int backgroundColor = currentItemTag.equals(mNavigationItem.tag)
                        ? mColorActive
                        : mColorInactive;
                mNavigationItemLayout.setBackgroundColor(backgroundColor);
            });
        }

        boolean hasView() {
            return mView != null;
        }

        View getView() {
            return mView;
        }
    }
}
