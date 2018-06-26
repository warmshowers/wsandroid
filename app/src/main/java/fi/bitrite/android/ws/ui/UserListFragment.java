package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.listadapter.UserListAdapter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class UserListFragment extends BaseFragment {

    private static final String KEY_USER_IDS = "user_ids";

    @Inject UserRepository mUserRepository;

    @BindView(R.id.users_lbl_multiple_user_address) TextView mLblMultipleUserAddress;
    @BindView(R.id.users_lbl_users_at_address) TextView mLblUsersAtAddress;
    @BindView(R.id.users_lst_users) ListView mLstUsers;

    public static Fragment create(ArrayList<Integer> userIds) {
        Bundle bundle = new Bundle();
        bundle.putIntegerArrayList(KEY_USER_IDS, userIds);

        Fragment fragment = new SearchFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);
        ButterKnife.bind(this, view);

        // Fetches the provided userId list.
        final Bundle args = getArguments();
        if (args == null || !args.containsKey(KEY_USER_IDS)) {
            throw new RuntimeException("UserListFragment requires a user list");
        }
        ArrayList<Integer> userIds = args.getIntegerArrayList(KEY_USER_IDS);
        userIds = userIds != null ? userIds : new ArrayList<>();

        // Populates the users to the user list.
        List<Observable<Resource<User>>> users = mUserRepository.get(userIds);
        UserListAdapter userListAdapter =
                new UserListAdapter(getContext(), UserListAdapter.COMPERATOR_FULLNAME_ASC, null);
        userListAdapter.resetDataset(users, 0);
        mLstUsers.setAdapter(userListAdapter);

        mLblUsersAtAddress.setText(getResources().getQuantityString(
                R.plurals.user_count, userIds.size(), userIds.size()));
        getCreateDestroyViewDisposable().add(userListAdapter.getUsers()
                .observeOn(AndroidSchedulers.mainThread())
                .firstElement()
                .subscribe(sortedUsers -> mLblMultipleUserAddress.setText(!sortedUsers.isEmpty()
                        ? sortedUsers.get(0).getFullAddress()
                        : "")));

        return view;
    }

    @OnItemClick(R.id.search_lst_result)
    public void onUserClicked(int position) {
        User user = (User) mLstUsers.getItemAtPosition(position);
        getNavigationController().navigateToUser(user.id);
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_users);
    }
}
