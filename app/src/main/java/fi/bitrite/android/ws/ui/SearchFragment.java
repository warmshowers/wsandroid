package fi.bitrite.android.ws.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.impl.RestTextSearch;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.ui.listadapter.UserListAdapter;
import fi.bitrite.android.ws.ui.util.DialogHelper;
import fi.bitrite.android.ws.ui.util.NavigationController;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.util.Tools;

public class SearchFragment extends BaseFragment {

    private static final String KEY_QUERY = "query";
    private static final String KEY_SEARCH_RESULT = "search_result";

    @Inject AuthenticationController mAuthenticationController;
    @Inject NavigationController mNavigationController;

    @BindView(R.id.all_lbl_no_network_warning) TextView mLblNoNetwork;
    @BindView(R.id.search_layout_summary) LinearLayout mLayoutSummary;
    @BindView(R.id.search_lbl_multiple_user_address) TextView mLblMultipleUserAddress;
    @BindView(R.id.search_lbl_users_at_address) TextView mLblUsersAtAddress;
    @BindView(R.id.search_lst_result) ListView mLstSearchResult;

    private UserListAdapter mSearchResultListAdapter;
    private TextSearchTask mTextSearchTask;

    private ArrayList<Host> mSearchResult;
    private String mQuery;

    private ProgressDialog.Disposable mProgressDisposable;
    private boolean mSearchSuccessful;

    public static Fragment create(ArrayList<Host> users) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(KEY_SEARCH_RESULT, users);

        Fragment fragment = new SearchFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static Fragment create(String query) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_QUERY, query);

        Fragment fragment = new SearchFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, view);

        // Checks if we already have the search result.
        final Bundle args = getArguments();
        ArrayList<Host> argsProvidedSearchResult =
                args.getParcelableArrayList(KEY_SEARCH_RESULT);
        ArrayList<Host> icicleProvidedSearchResult = savedInstanceState == null
                ? null
                : savedInstanceState.getParcelableArrayList(KEY_SEARCH_RESULT);
        mSearchResult = argsProvidedSearchResult == null
                ? icicleProvidedSearchResult
                : argsProvidedSearchResult;
        mSearchSuccessful = mSearchResult != null;
        mSearchResult = mSearchResult != null ? mSearchResult : new ArrayList<>();

        // TODO(saemy): Move to own fragment (which has a common base to this one).
        boolean hasArgsProvidedSearchResult = argsProvidedSearchResult != null;
        mLayoutSummary.setVisibility(hasArgsProvidedSearchResult ? View.VISIBLE : View.GONE);
        mLblUsersAtAddress.setText(hasArgsProvidedSearchResult
                ? getResources().getQuantityString(
                    R.plurals.host_count, mSearchResult.size(), mSearchResult.size())
                : "");
        mLblMultipleUserAddress.setText(hasArgsProvidedSearchResult
                ? mSearchResult.get(0).getLocation()
                : "");

        // Does the requested search if it did not finish yet.
        mQuery = getArguments().getString(KEY_QUERY);
        if (mQuery != null) {
            if (!mSearchSuccessful) {
                doTextSearch(mQuery);
            }
        }

        // Initializes the search result list.
        mSearchResultListAdapter = new UserListAdapter(getContext(), mQuery, mSearchResult);
        mLstSearchResult.setAdapter(mSearchResultListAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isConnected = Tools.isNetworkConnected(getContext());
        mLblNoNetwork.setVisibility(isConnected ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (mSearchSuccessful) {
            outState.putParcelableArrayList(KEY_SEARCH_RESULT, mSearchResult);
        }

        if (mTextSearchTask != null) {
            mTextSearchTask.cancel(true);
        }
        super.onSaveInstanceState(outState);
    }

    private void doTextSearch(String text) {
        mProgressDisposable = ProgressDialog.create(R.string.performing_search)
                .show(getActivity());

        Search search = new RestTextSearch(mAuthenticationController, text);
        mTextSearchTask = new TextSearchTask();
        mTextSearchTask.execute(search);
    }

    @OnItemClick(R.id.search_lst_result)
    public void onUserClicked(int position) {
        Host user = (Host) mLstSearchResult.getItemAtPosition(position);
        mNavigationController.navigateToUser(user.getId());
    }

    private class TextSearchTask extends AsyncTask<Search, Void, Object> {

        @Override
        protected Object doInBackground(Search... params) {
            try {
                Search search = params[0];
                return search.doSearch();
            } catch (Exception e) {
                Log.e(WSAndroidApplication.TAG, e.getMessage(), e);
                return e;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Exception) {
                mProgressDisposable.dispose();

                RestClient.reportError(getContext(), result);
                return;
            }

            mSearchResult = (ArrayList<Host>) result;

            // Sort so that available hosts come up first
            Collections.sort(mSearchResult, (left, right) -> {
                int ncaLeft = left.isNotCurrentlyAvailable() ? 1  : 0;
                int ncaRight = right.isNotCurrentlyAvailable() ? 1 : 0;

                return ncaLeft != ncaRight
                        ? ncaLeft - ncaRight
                        : left.getFullname().compareTo(right.getFullname()); // TODO(saemy): Something smarter?
            });

            mSearchResultListAdapter.resetDataset(mSearchResult);

            mProgressDisposable.dispose();

            if (mSearchResult.isEmpty()) {
                DialogHelper.alert(getContext(), R.string.no_results);
            }

            mSearchSuccessful = true;
        }
    }

    @Override
    protected CharSequence getTitle() {
        CharSequence title = getString(R.string.title_fragment_search);
        if (mQuery != null) {
            title = title + ": \"" + mQuery + "\"";
        }
        return title;
    }
}
