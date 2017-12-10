package fi.bitrite.android.ws.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.impl.RestTextSearch;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.Tools;

public class ListSearchTabActivity
        extends WSBaseActivity
        implements android.widget.AdapterView.OnItemClickListener, Injectable {

    public static final String SEARCH_SUCCESSFUL = "ListSearchTab_Search_Successful";
    public static final String CLUSTER_MEMBERS = "search_results";

    @Inject AuthenticationController mAuthenticationController;

    HostListAdapter mHostListAdapter;

    ArrayList<HostBriefInfo> mListSearchHosts = new ArrayList<>();
    String mQuery = "";

    TextView mNoNetworkWarning;
    ListView mListSearchResult;
    DialogHandler mDialogHandler;
    TextSearchTask mTextSearchTask;
    LinearLayout mSearchEditLayout;
    LinearLayout mSearchResultsLayout;
    TextView mMultipleHostsAddress;
    TextView mHostsAtAddress;

    boolean mSearchSuccessful;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_tab);

        // Tell the BaseActivity to use a back action in the toolbar instead of the hamburger
        mHasBackIntent = true;

        initView();

        mDialogHandler = new DialogHandler(this);

        mNoNetworkWarning = (TextView) findViewById(R.id.noNetworkWarningList);
        mListSearchResult = (ListView) findViewById(R.id.lstSearchResult);
        mSearchResultsLayout = (LinearLayout) findViewById(R.id.listSummaryLayout);
        mMultipleHostsAddress = (TextView) findViewById(R.id.multipleHostAddress);
        mHostsAtAddress = (TextView) findViewById(R.id.hostsAtAddress);

        mListSearchResult.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(ListSearchTabActivity.this, HostInformationActivity.class);
                HostBriefInfo briefInfo = (HostBriefInfo) mListSearchResult.getItemAtPosition(position);
                Host host = Host.createFromBriefInfo(briefInfo);
                i.putExtra("host", host);
                i.putExtra("id", briefInfo.getId());
                startActivityForResult(i, 0);
            }
        });

        mHostListAdapter = new HostListAdapter(
                WSAndroidApplication.getAppContext(),
                R.layout.host_list_item,
                mQuery,
                mListSearchHosts
        );
        mListSearchResult.setAdapter(mHostListAdapter);

        if (savedInstanceState != null) {
            mListSearchHosts = savedInstanceState.getParcelableArrayList("list_search_hosts");

            // Updates the dataset of the adapter with the hosts saved in mListSearchHosts
            mHostListAdapter.resetDataset(mListSearchHosts);

            mSearchSuccessful = savedInstanceState.getBoolean(SEARCH_SUCCESSFUL);
        }

        handleIntent(getIntent());

        if (mQuery != null) {
            getSupportActionBar().setTitle(getSupportActionBar().getTitle() + ": \"" + mQuery + "\"");
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mQuery = intent.getStringExtra(SearchManager.QUERY);

            if (!mSearchSuccessful) {
                doTextSearch(mQuery);
            }

        } else if (intent.hasExtra(CLUSTER_MEMBERS)) {
            mListSearchHosts = intent.getParcelableArrayListExtra(CLUSTER_MEMBERS);
            if (!mListSearchHosts.isEmpty()) {
                // Hide the SearchEdit
                mSearchEditLayout.setVisibility(View.INVISIBLE);
                mSearchEditLayout.getLayoutParams().height = 0;
                // Show the search results header
                mSearchResultsLayout.setVisibility(View.VISIBLE);
                mSearchResultsLayout.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

                mMultipleHostsAddress.setText(mListSearchHosts.get(0).getLocation());
                mHostsAtAddress.setText(getResources().getQuantityString(R.plurals.host_count, mListSearchHosts.size(), mListSearchHosts.size()));
            }
        }
    }

    private void showProgressDialog() {
        FragmentManager fm = getSupportFragmentManager();
        ProgressDialogFragment progressDialogFragment =
                ProgressDialogFragment.newInstance(R.string.performing_search);
        progressDialogFragment.show(fm, "fragment_progress");
    }

    private void dismissProgressDialog() {
        final FragmentManager fm = getSupportFragmentManager();

        final Fragment progress = fm.findFragmentByTag("fragment_progress");
        if (progress != null) {
            ((DialogFragment) progress).dismiss();
        }
    }

    public void doTextSearch(String text) {
        showProgressDialog();
        Search search = new RestTextSearch(mAuthenticationController, text);
        mTextSearchTask = new TextSearchTask();
        mTextSearchTask.execute(search);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        super.onItemClick(parent, view, position, id);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("list_search_hosts", mListSearchHosts);
        outState.putBoolean(SEARCH_SUCCESSFUL, mSearchSuccessful);

        if (mTextSearchTask != null) {
            mTextSearchTask.cancel(true);
        }
        super.onSaveInstanceState(outState);
    }

    private class TextSearchTask extends AsyncTask<Search, Void, Object> {

        @Override
        protected Object doInBackground(Search... params) {
            Search search = params[0];
            Object retObj;

            try {
                retObj = search.doSearch();
            } catch (Exception e) {
                Log.e(WSAndroidApplication.TAG, e.getMessage(), e);
                retObj = e;
            }

            return retObj;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Object result) {
            dismissProgressDialog();

            if (result instanceof Exception) {
                RestClient.reportError(ListSearchTabActivity.this, result);
                return;
            }

            mListSearchHosts = (ArrayList<HostBriefInfo>) result;
            // Sort so that available hosts come up first
            Collections.sort(mListSearchHosts, new Comparator<HostBriefInfo>() {
                public int compare(HostBriefInfo h1, HostBriefInfo h2) {
                    return h1.getNotCurrentlyAvailableAsInt() - h2.getNotCurrentlyAvailableAsInt();
                }
            });

            mHostListAdapter.resetDataset(mListSearchHosts);

            if (mListSearchHosts.isEmpty()) {
                mDialogHandler.alert(getResources().getString(R.string.no_results));
            }

            mSearchSuccessful = true;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Tools.isNetworkConnected(this)) {
            mNoNetworkWarning.setText(getString(R.string.not_connected_to_network));
            return;
        }
        mNoNetworkWarning.setText("");
        mNoNetworkWarning.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

}
