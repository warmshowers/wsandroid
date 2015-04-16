package fi.bitrite.android.ws.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.google.android.gms.analytics.GoogleAnalytics;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.impl.RestTextSearch;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.Tools;

import java.util.ArrayList;

public class ListSearchTabActivity extends WSBaseActivity {

    ArrayList<HostBriefInfo> mListSearchHosts;

    TextView mNoNetworkWarning;
    EditText mListSearchEdit;
    ImageView mListSearchButton;
    ListView mListSearchResult;
    DialogHandler mDialogHandler;
    TextSearchTask mTextSearchTask;
    LinearLayout mSearchEditLayout;
    LinearLayout mSearchResultsLayout;
    TextView mMultipleHostsAddress;
    TextView mHostsAtAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.list_tab);

        // Because the parent class accesses R.id.* we have to have a view before calling super.onCreate()
        super.onCreate(savedInstanceState);

        mDialogHandler = new DialogHandler(this);

        mNoNetworkWarning = (TextView) findViewById(R.id.noNetworkWarningList);
        mListSearchEdit = (EditText) findViewById(R.id.editListSearch);
        mListSearchButton = (ImageView) findViewById(R.id.btnListSearch);
        mListSearchResult = (ListView) findViewById(R.id.lstSearchResult);
        mSearchEditLayout = (LinearLayout) findViewById(R.id.searchEditLayout);
        mSearchResultsLayout = (LinearLayout) findViewById(R.id.listSummaryLayout);
        mMultipleHostsAddress = (TextView) findViewById(R.id.multipleHostAddress);
        mHostsAtAddress = (TextView) findViewById(R.id.hostsAtAddress);

        setupListSearch(savedInstanceState);

        mToolbar.setTitle(getString(R.string.text_search_title));
    }

    private void setupListSearch(Bundle savedInstanceState) {
        mListSearchEdit.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    startSearchUsingEditFieldInput();
                }

                return true;
            }
        });

        mListSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startSearchUsingEditFieldInput();
            }
        });

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

        // Hide the SearchResults header by default
        mSearchResultsLayout.setVisibility(View.INVISIBLE);
        mSearchResultsLayout.getLayoutParams().height = 0;

        Intent receivedIntent = getIntent();
        if (receivedIntent.hasExtra("search_results")) {
            mListSearchHosts = receivedIntent.getParcelableArrayListExtra("search_results");
            if (!mListSearchHosts.isEmpty()) {
                // Hide the SearchEdit
                mSearchEditLayout.setVisibility(View.INVISIBLE);
                mSearchEditLayout.getLayoutParams().height = 0;
                // Show the search results header
                mSearchResultsLayout.setVisibility(View.VISIBLE);
                mSearchResultsLayout.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

                mMultipleHostsAddress.setText(mListSearchHosts.get(0).getLocation());
                mHostsAtAddress.setText(getString(R.string.host_count, mListSearchHosts.size()));
            }
        } else if (savedInstanceState != null) {
            mListSearchHosts = savedInstanceState.getParcelableArrayList("list_search_hosts");
        }
        boolean inProgress = DialogHandler.inProgress();
        if (mListSearchHosts != null) {
            mListSearchResult.setAdapter(new HostListAdapter(WSAndroidApplication.getAppContext(),
                    R.layout.host_list_item, mListSearchHosts));
        }

        if (inProgress) {
            mDialogHandler.dismiss();
            doTextSearch(savedInstanceState.getString("search_text"));
        }
    }

    protected void startSearchUsingEditFieldInput() {
        hideKeyboard();
        doTextSearch(mListSearchEdit.getText().toString());
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mListSearchEdit.getWindowToken(), 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        return mDialogHandler.createDialog(id, getResources().getString(R.string.performing_search));
    }

    public void doTextSearch(String text) {
        mDialogHandler.showDialog(DialogHandler.TEXT_SEARCH);
        Search search = new RestTextSearch(text);
        mTextSearchTask = new TextSearchTask();
        mTextSearchTask.execute(search);
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
            mDialogHandler.dismiss();

            if (result instanceof Exception) {
                RestClient.reportError(ListSearchTabActivity.this, result);
                return;
            }

            mListSearchHosts = (ArrayList<HostBriefInfo>) result;
            mListSearchResult.setAdapter(new HostListAdapter(WSAndroidApplication.getAppContext(),
                    R.layout.host_list_item, mListSearchHosts));

            if (mListSearchHosts.isEmpty()) {
                mDialogHandler.alert(getResources().getString(R.string.no_results));
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("list_search_hosts", mListSearchHosts);
        if (DialogHandler.inProgress() && mTextSearchTask != null) {
            outState.putString("search_text", mListSearchEdit.getText().toString());
            mTextSearchTask.cancel(true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Tools.isNetworkConnected(this)) {
            mNoNetworkWarning.setText(getString(R.string.not_connected_to_network));
            mListSearchEdit.setEnabled(false);
            return;
        }
        mListSearchEdit.setEnabled(true);
        mNoNetworkWarning.setText("");
        mNoNetworkWarning.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();
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
