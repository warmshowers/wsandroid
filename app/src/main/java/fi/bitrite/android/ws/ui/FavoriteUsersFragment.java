package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.repository.FavoriteRepository;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.listadapter.UserListAdapter;
import fi.bitrite.android.ws.ui.util.NavigationController;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class FavoriteUsersFragment extends BaseFragment {

    private static final int CONTEXT_MENU_DELETE = 0;

    @Inject FavoriteRepository mFavoriteRepository;
    @Inject NavigationController mNavigationController;
    @Inject UserRepository mUserRepository;

    @BindView(R.id.favorites_lst_users) ListView mLstUsers;
    @BindView(R.id.favorites_lbl_no_users) TextView mLblNoUsers;

    private UserListAdapter mUserListAdapter;
    private List<Host> mFavoriteUsers = new ArrayList<>();

    public static Fragment create() {
        Bundle bundle = new Bundle();

        Fragment fragment = new FavoriteUsersFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite_users, container, false);
        ButterKnife.bind(this, view);

        mUserListAdapter = new UserListAdapter(getContext(), null, mFavoriteUsers);
        mLstUsers.setAdapter(mUserListAdapter);

        registerForContextMenu(mLstUsers);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFavoriteUsersList();
    }

    @OnItemClick(R.id.favorites_lst_users)
    public void onUserClicked(int position) {
        Host selectedUser = mFavoriteUsers.get(position);
        mNavigationController.navigateToUser(selectedUser.getId());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        if (view.getId() == mLstUsers.getId()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(mFavoriteUsers.get(info.position).getFullname());
            menu.add(Menu.NONE, CONTEXT_MENU_DELETE, 0, R.string.delete);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_MENU_DELETE:
                Host user = mFavoriteUsers.get(info.position);
                mFavoriteRepository.remove(user.getId());
                updateFavoriteUsersList();
                return true;
        }

        return super.onContextItemSelected(item);
    }

    private void updateFavoriteUsersList() {
        List<Observable<Resource<Host>>> favorites = mFavoriteRepository.getFavorites();

        Map<Integer, Host> hosts = new HashMap<>();
        Observable.merge(favorites)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(hostResource -> {
                    if (hostResource.data != null) {
                        hosts.put(hostResource.data.getId(), hostResource.data);

                        if (hosts.size() == favorites.size()) {
                            mFavoriteUsers = new ArrayList<>(hosts.values());

                            // Sort in order of name
                            Collections.sort(mFavoriteUsers, (left, right) -> left.getFullname().compareTo(right.getFullname()));

                            mUserListAdapter.resetDataset(mFavoriteUsers);
                        }
                    }
                });

        boolean hasFavorites = !favorites.isEmpty();
        mLblNoUsers.setVisibility(hasFavorites ? View.GONE : View.VISIBLE);
        mLstUsers.setVisibility(hasFavorites ? View.VISIBLE : View.GONE);
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_favorites);
    }
}
