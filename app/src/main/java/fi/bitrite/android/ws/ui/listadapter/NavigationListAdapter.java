package fi.bitrite.android.ws.ui.listadapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.ui.model.NavigationItem;
import fi.bitrite.android.ws.util.Tools;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class NavigationListAdapter extends ArrayAdapter<NavigationListAdapter.Item> {

    private final Observable<NavigationItem> mOnClickSubject;
    private Locale mLocale;

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
        @SuppressLint("ViewHolder") View view = LayoutInflater.from(getContext())
                .inflate(R.layout.item_navigation, parent, false);
        ItemInjector itemInjector = new ItemInjector(item, view);
        view.setTag(position);
        return view;
    }

    static class Item {
        final NavigationItem navigationItem;
        final BehaviorSubject<String> currentTag;
        final Subject<NavigationItem> onClickSubject;

        Item(NavigationItem navigationItem, BehaviorSubject<String> currentTag,
             Subject<NavigationItem> onClickSubject) {
            this.navigationItem = navigationItem;
            this.currentTag = currentTag;
            this.onClickSubject = onClickSubject;
        }
    }
    class ItemInjector {
        @BindColor(R.color.backgroundLightGrey) int mColorActive;
        @BindColor(android.R.color.transparent) int mColorInactive;

        @BindView(R.id.icon_menu_item) ImageView mIcon;
        @BindView(R.id.lbl_menu_item) TextView mLabel;
        @BindView(R.id.layout_navigation_item) ConstraintLayout mLayoutNavigationItem;
        @BindView(R.id.layout_menu_item_notification_count) FrameLayout mLayoutNotificationCount;
        @BindView(R.id.txt_menu_item_notification_count) TextView mTxtNotificationCount;

        private Item mItem;

        ItemInjector(Item item, View view) {
            mItem = item;

            if (mLocale == null) {
                mLocale = Tools.getLocale(view.getContext().getApplicationContext());
            }

            ButterKnife.bind(this, view);
            mIcon.setImageResource(item.navigationItem.iconResourceId);
            mLabel.setText(item.navigationItem.labelResourceId);

            Disposable mUnused1 = item.currentTag.subscribe(currentItemTag -> {
                int backgroundColor = currentItemTag.equals(item.navigationItem.tag)
                        ? mColorActive
                        : mColorInactive;
                mLayoutNavigationItem.setBackgroundColor(backgroundColor);
            });

            Disposable mUnused2 = item.navigationItem.notificationCount.subscribe(notificationCount -> {
                mLayoutNotificationCount.setVisibility(
                        notificationCount > 0 ? View.VISIBLE : View.GONE);
                mTxtNotificationCount.setText(String.format(mLocale, "%d", notificationCount));
            });
        }

        @OnClick(R.id.layout_navigation_item)
        public void onClick() {
            // Informs the observers about this click event.
            mItem.onClickSubject.onNext(mItem.navigationItem);
        }
    }
}
