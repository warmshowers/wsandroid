package fi.bitrite.android.ws.ui.listadapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.ui.widget.UserCircleImageView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class UserListAdapter extends ArrayAdapter<Host> {

    public final static Comparator<? super Host> COMPERATOR_FULLNAME_ASC =
            (left, right) -> left.getFullname().compareTo(right.getFullname());

    private final Comparator<? super Host> mComparator;
    private final Decorator mDecorator;

    private BehaviorSubject<List<Host>> mUsers = BehaviorSubject.create();

    @BindView(R.id.user_list_layout) LinearLayout mLayout;
    @BindView(R.id.user_list_icon) UserCircleImageView mIcon;
    @BindView(R.id.user_list_lbl_fullname) TextView mLblFullname;
    @BindView(R.id.user_list_lbl_location) TextView mLblLocation;
    @BindView(R.id.user_list_lbl_member_info) TextView mMemberInfo;

    public UserListAdapter(@NonNull Context context, @Nullable Comparator<? super Host> comparator,
                           @Nullable Decorator decorator) {
        super(context, R.layout.item_user_list);

        mComparator = comparator;
        mDecorator = decorator;

        mUsers.observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::resetDataset);
    }

    public void resetDataset(List<Observable<Resource<Host>>> users, Object ignored) {
        @SuppressLint("UseSparseArrays") // We need loadedUsers.values()
        final Map<Integer, Host> loadedUsers = new HashMap<>();

        Observable.merge(users)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userResource -> {
                    final Host user = userResource.data;
                    if (user != null) {
                        loadedUsers.put(user.getId(), user);
                        mUsers.onNext(new ArrayList<>(loadedUsers.values()));
                    }
                });
    }

    public void resetDataset(List<Host> users) {
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
        final Host user = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_user_list, parent, false);
        }

        ButterKnife.bind(this, convertView);

        // Decorates the shown fields.
        if (mDecorator != null) {
            mLblFullname.setText(mDecorator.decorateFullname(user.getFullname()));
            mLblLocation.setText(mDecorator.decorateLocation(user.getLocation()));
        } else {
            mLblFullname.setText(user.getFullname());
            mLblLocation.setText(user.getLocation());
        }

        // Greys out unavailable users.
        mLayout.setAlpha(user.isNotCurrentlyAvailable() ? 0.5f : 1.0f);

        mIcon.setUser(user);

        DateFormat simpleDate = DateFormat.getDateInstance();
        String activeDate = simpleDate.format(user.getLastLoginAsDate());
        String createdDate =
                new SimpleDateFormat("yyyy", Locale.US).format(user.getCreatedAsDate());

        String memberString =
                getContext().getString(R.string.search_host_summary, createdDate, activeDate);
        mMemberInfo.setText(memberString);

        return convertView;
    }

    public BehaviorSubject<List<Host>> getUsers() {
        return mUsers;
    }

    @Nullable
    public Host getUser(int pos) {
        List<Host> users = mUsers.getValue();
        return users != null && users.size() > pos
                ? users.get(pos)
                : null;
    }

    public interface Decorator {
        SpannableStringBuilder decorateFullname(String fullname);
        SpannableStringBuilder decorateLocation(String location);
    }
}
