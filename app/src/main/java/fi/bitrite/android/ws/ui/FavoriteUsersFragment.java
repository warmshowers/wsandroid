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
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;
import fi.bitrite.android.ws.ui.listadapter.UserListAdapter;
import fi.bitrite.android.ws.ui.util.NavigationController;

public class FavoriteUsersFragment extends BaseFragment {

    private static final int CONTEXT_MENU_DELETE = 0;

    @Inject NavigationController mNavigationController;

    @BindView(R.id.favorites_lst_users) ListView mLstUsers;
    @BindView(R.id.favorites_lbl_no_users) TextView mLblNoUsers;

    private final StarredHostDao mFavoriteUsersDao = new StarredHostDaoImpl();
    private UserListAdapter mUserListAdapter;
    private List<HostBriefInfo> mFavoriteUsers = new ArrayList<>();

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
        mFavoriteUsersDao.open();
        updateFavoriteUsersList();
    }

    @Override 
    public void onPause() {
        super.onPause();
        mFavoriteUsersDao.close();
    }

    @OnItemClick(R.id.favorites_lst_users)
    public void onUserClicked(int position) {
        HostBriefInfo selectedUser = mFavoriteUsers.get(position);
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
                HostBriefInfo host = mFavoriteUsers.get(info.position);
                mFavoriteUsersDao.delete(host.getId(), host.getName());
                updateFavoriteUsersList();
                return true;
        }

        return super.onContextItemSelected(item);
    }

    private void updateFavoriteUsersList() {
        mFavoriteUsers = mFavoriteUsersDao.getAllBrief();
        // Sort in order of recently saved
        Collections.sort(mFavoriteUsers, (left, right) -> (int)(right.getUpdated() - left.getUpdated()));

        boolean hasFavorites = !mFavoriteUsers.isEmpty();

        mLblNoUsers.setVisibility(hasFavorites ? View.GONE : View.VISIBLE);
        mLstUsers.setVisibility(hasFavorites ? View.VISIBLE : View.GONE);

        mUserListAdapter.resetDataset(mFavoriteUsers);
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_favorites);
    }
}
