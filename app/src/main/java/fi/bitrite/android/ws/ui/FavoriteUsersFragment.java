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

import java.util.List;

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
import io.reactivex.Observable;

public class FavoriteUsersFragment extends BaseFragment {

    private static final int CONTEXT_MENU_DELETE = 0;

    @Inject FavoriteRepository mFavoriteRepository;
    @Inject UserRepository mUserRepository;

    @BindView(R.id.favorites_lst_users) ListView mLstUsers;
    @BindView(R.id.favorites_rellayout_no_users) View mLblNoUsers;

    private UserListAdapter mUserListAdapter;

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

        mUserListAdapter = new UserListAdapter(
                getContext(), UserListAdapter.COMPERATOR_FULLNAME_ASC, null);
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
        Host selectedUser = mUserListAdapter.getUser(position);
        getNavigationController().navigateToUser(selectedUser.getId());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        if (view.getId() == mLstUsers.getId()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Host user = mUserListAdapter.getUser(info.position);

            menu.setHeaderTitle(user.getFullname());
            menu.add(Menu.NONE, CONTEXT_MENU_DELETE, 0, R.string.delete);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_MENU_DELETE:
                Host user = mUserListAdapter.getUser(info.position);
                mFavoriteRepository.remove(user.getId());
                updateFavoriteUsersList();
                return true;
        }

        return super.onContextItemSelected(item);
    }

    private void updateFavoriteUsersList() {
        List<Observable<Resource<Host>>> favorites = mFavoriteRepository.getFavorites();
        mUserListAdapter.resetDataset(favorites, 0);

        boolean hasFavorites = !favorites.isEmpty();
        mLblNoUsers.setVisibility(hasFavorites ? View.GONE : View.VISIBLE);
        mLstUsers.setVisibility(hasFavorites ? View.VISIBLE : View.GONE);
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_favorites);
    }
}
