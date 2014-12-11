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
import fi.bitrite.android.ws.host.SearchFactory;
import fi.bitrite.android.ws.host.impl.WsSearchFactory;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpException;
import org.json.JSONException;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

import java.util.ArrayList;

public class ListSearchTabActivity extends RoboActivity {

    @InjectView(R.id.editListSearch)
    EditText listSearchEdit;
    @InjectView(R.id.btnListSearch)
    ImageView listSearchButton;
    @InjectView(R.id.lstSearchResult)
    ListView listSearchResult;

    SearchFactory searchFactory = new WsSearchFactory();

    private ArrayList<HostBriefInfo> listSearchHosts;

    private DialogHandler dialogHandler;
    private TextSearchTask textSearchTask;
    private LinearLayout mSearchEditLayout;
    private LinearLayout mSearchResultsLayout;
    private TextView mMultipleHostsAddress;
    private TextView mHostsAtAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_tab);
        dialogHandler = new DialogHandler(this);

        mSearchEditLayout = (LinearLayout) findViewById(R.id.searchEditLayout);
        mSearchResultsLayout = (LinearLayout) findViewById(R.id.listSummaryLayout);
        mMultipleHostsAddress = (TextView) findViewById(R.id.multipleHostAddress);
        mHostsAtAddress = (TextView) findViewById(R.id.hostsAtAddress);

        setupListSearch(savedInstanceState);
    }

    private void setupListSearch(Bundle savedInstanceState) {
        listSearchEdit.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    startSearchUsingEditFieldInput();
                }

                return true;
            }
        });

        listSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startSearchUsingEditFieldInput();
            }
        });

        listSearchResult.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(ListSearchTabActivity.this, HostInformationActivity.class);
                HostBriefInfo briefInfo = (HostBriefInfo) listSearchResult.getItemAtPosition(position);
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
            listSearchHosts = receivedIntent.getParcelableArrayListExtra("search_results");
            if (!listSearchHosts.isEmpty()) {
                // Hide the SearchEdit
                mSearchEditLayout.setVisibility(View.INVISIBLE);
                mSearchEditLayout.getLayoutParams().height = 0;
                // Show the search results header
                mSearchResultsLayout.setVisibility(View.VISIBLE);
                mSearchResultsLayout.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

                mMultipleHostsAddress.setText(listSearchHosts.get(0).getLocation());
                mHostsAtAddress.setText(getString(R.string.host_count, listSearchHosts.size()));
            }
        } else if (savedInstanceState != null) {
            listSearchHosts = savedInstanceState.getParcelableArrayList("list_search_hosts");
        }
        boolean inProgress = DialogHandler.inProgress();
        if (listSearchHosts != null) {
            listSearchResult.setAdapter(new HostListAdapter(WSAndroidApplication.getAppContext(),
                    R.layout.host_list_item, listSearchHosts));
        }

        if (inProgress) {
            dialogHandler.dismiss();
            doTextSearch(savedInstanceState.getString("search_text"));
        }
    }

    protected void startSearchUsingEditFieldInput() {
        hideKeyboard();
        doTextSearch(listSearchEdit.getText().toString());
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(listSearchEdit.getWindowToken(), 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        return dialogHandler.createDialog(id, getResources().getString(R.string.performing_search));
    }

    public void doTextSearch(String text) {
        dialogHandler.showDialog(DialogHandler.TEXT_SEARCH);
        Search search = searchFactory.createTextSearch(text);
        textSearchTask = new TextSearchTask();
        textSearchTask.execute(search);
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
            dialogHandler.dismiss();

            if (result instanceof Exception) {
                RestClient.reportError(ListSearchTabActivity.this, result);
                return;
            }

            listSearchHosts = (ArrayList<HostBriefInfo>) result;
            listSearchResult.setAdapter(new HostListAdapter(WSAndroidApplication.getAppContext(),
                    R.layout.host_list_item, listSearchHosts));

            if (listSearchHosts.isEmpty()) {
                dialogHandler.alert(getResources().getString(R.string.no_results));
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("list_search_hosts", listSearchHosts);
        if (DialogHandler.inProgress()) {
            outState.putString("search_text", listSearchEdit.getText().toString());
            textSearchTask.cancel(true);
        }
        super.onSaveInstanceState(outState);
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
