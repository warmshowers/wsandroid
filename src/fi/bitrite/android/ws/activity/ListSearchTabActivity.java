package fi.bitrite.android.ws.activity;

import java.util.ArrayList;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.SearchFactory;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.MapAnimator;

public class ListSearchTabActivity extends RoboActivity {

	@InjectView(R.id.listTab) LinearLayout listTab;
	@InjectView(R.id.editListSearch) EditText listSearchEdit;
	@InjectView(R.id.btnListSearch) ImageView listSearchButton;
	@InjectView(R.id.lstSearchResult) ListView listSearchResult;

	@Inject SearchFactory searchFactory;
	
	@Inject MapAnimator mapAnimator;

	private ArrayList<HostBriefInfo> listSearchHosts;
	
	private DialogHandler dialogHandler;
	private TextSearchTask textSearchTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_tab);
		dialogHandler = new DialogHandler(this);
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
				startActivityForResult(i, 0);
			}
		});
		
		if (savedInstanceState != null) {
			listSearchHosts = savedInstanceState.getParcelableArrayList("list_search_hosts");
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
		if (resultCode == HostInformationActivity.RESULT_SHOW_HOST_ON_MAP) {
			MainActivity parent = (MainActivity) this.getParent();
			mapAnimator.prepareToAnimateToHost(data);
			parent.switchTab(2);
		}
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
			Object retObj = null;
			
			try {
				retObj = search.doSearch();
			}
				
			catch (Exception e) {
				Log.e("WSAndroid", e.getMessage(), e);
				retObj = e;
			}
			
			return retObj;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected void onPostExecute(Object result) {
			dialogHandler.dismiss();
			
			if (result instanceof Exception) {
				dialogHandler.alert(getResources().getString(R.string.error_retrieving_host_information));
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
}
