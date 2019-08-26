package fi.bitrite.android.ws.ui.listadapter;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.ui.widget.UserCircleImageView;
import fi.bitrite.android.ws.util.Tools;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class UserListAdapter extends ArrayAdapter<SimpleUser> {

    public final static Comparator<? super SimpleUser> COMPERATOR_FULLNAME_ASC =
            (left, right) -> left.getName().compareTo(right.getName());

    private final Comparator<? super SimpleUser> mComparator;
    private final Decorator mDecorator;

    private BehaviorSubject<List<? extends SimpleUser>> mUsers = BehaviorSubject.create();

    @BindView(R.id.user_list_layout) LinearLayout mLayout;
    @BindView(R.id.user_list_icon) UserCircleImageView mIcon;
    @BindView(R.id.user_list_lbl_fullname) TextView mLblFullname;
    @BindView(R.id.user_list_lbl_location) TextView mLblLocation;
    @BindView(R.id.user_list_lbl_member_info) TextView mMemberInfo;

    public UserListAdapter(@NonNull Context context,
                           @Nullable Comparator<? super SimpleUser> comparator,
                           @Nullable Decorator decorator) {
        super(context, R.layout.item_user_list);

        mComparator = comparator;
        mDecorator = decorator;

        final Disposable unused = mUsers
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::resetDataset);
    }

    public void resetDataset(List<Observable<Resource<User>>> users, Object ignored) {
        @SuppressLint("UseSparseArrays") // We need loadedUsers.values()
        final Map<Integer, User> loadedUsers = new HashMap<>();

        final Disposable unused = Observable.merge(users)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userResource -> {
                    final User user = userResource.data;
                    if (user != null) {
                        loadedUsers.put(user.id, user);
                        mUsers.onNext(new ArrayList<>(loadedUsers.values()));
                    }
                });
    }

    public void resetDataset(List<? extends SimpleUser> users) {
        if (mComparator != null) {
            Collections.sort(users, mComparator);
        }

        clear();
        addAll(users);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final SimpleUser user = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_user_list, parent, false);
        }

        ButterKnife.bind(this, convertView);

        // Decorates the shown fields.
        if (mDecorator != null) {
            mLblFullname.setText(mDecorator.decorateFullname(user.getName()));
            mLblLocation.setText(mDecorator.decorateLocation(user.getFullAddress()));
        } else {
            mLblFullname.setText(user.getName());
            mLblLocation.setText(user.getFullAddress());
        }

        // Greys out unavailable users.
        mLayout.setAlpha(user.isCurrentlyAvailable ? 1.0f : 0.5f);

        mIcon.setUser(user);

        DateFormat simpleDate = DateFormat.getDateInstance();
        String activeDate = simpleDate.format(user.lastAccess);
        String createdDate =
                new SimpleDateFormat("yyyy", Tools.getLocale(getContext())).format(user.created);

        String memberString =
                getContext().getString(R.string.search_user_summary, createdDate, activeDate);
        mMemberInfo.setText(memberString);

        return convertView;
    }

    public BehaviorSubject<List<? extends SimpleUser>> getUsers() {
        return mUsers;
    }

    @Nullable
    public SimpleUser getUser(int pos) {
        List<? extends SimpleUser> users = mUsers.getValue();
        return users != null && users.size() > pos
                ? users.get(pos)
                : null;
    }

    public interface Decorator {
        SpannableStringBuilder decorateFullname(String fullname);
        SpannableStringBuilder decorateLocation(String location);
    }
}
