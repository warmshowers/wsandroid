package fi.bitrite.android.ws.activity;

import java.util.ArrayList;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.host.SearchFactory;
import fi.bitrite.android.ws.host.SearchThread;
import fi.bitrite.android.ws.host.impl.MapAnimator;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;

public class ListSearchTabActivity extends RoboActivity {

	@InjectView(R.id.listTab) LinearLayout listTab;
	@InjectView(R.id.editListSearch) EditText listSearchEdit;
	@InjectView(R.id.btnListSearch) ImageView listSearchButton;
	@InjectView(R.id.lstSearchResult) ListView listSearchResult;

	@Inject SearchFactory searchFactory;
	
	@Inject MapAnimator mapAnimator;

	private ArrayList<HostBriefInfo> listSearchHosts;
	
	private DialogHandler dialogHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_tab);

		setupListSearch(savedInstanceState);
		
		dialogHandler = new DialogHandler(this);
	}

	private void setupListSearch(Bundle savedInstanceState) {
		listSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(listSearchEdit.getWindowToken(), 0);
				dialogHandler.showDialog(DialogHandler.TEXT_SEARCH);
				doTextSearch();
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
			if (listSearchHosts != null) {
				listSearchResult.setAdapter(new HostListAdapter(WSAndroidApplication.getAppContext(),
						R.layout.host_list_item, listSearchHosts));
			}
		}
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
		return dialogHandler.createDialog(id, "Performing search ...");
	}

	public void doTextSearch() {
		Search search = searchFactory.createTextSearch(listSearchEdit.getText().toString());
		new SearchThread(handler, search).start();
	}

	final Handler handler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			dialogHandler.dismiss();

			Object obj = msg.obj;

			if (obj instanceof Exception) {
				dialogHandler.alert("Search failed. Check your credentials and internet connection.");
				return;
			}

			listSearchHosts = (ArrayList<HostBriefInfo>) obj;

			if (listSearchHosts.isEmpty()) {
				dialogHandler.alert("Your search yielded no results.");
				return;
			}

			listSearchResult.setAdapter(new HostListAdapter(WSAndroidApplication.getAppContext(),
					R.layout.host_list_item, listSearchHosts));
		}
	};
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO: save dialog state?	
		outState.putParcelableArrayList("list_search_hosts", listSearchHosts); 
		super.onSaveInstanceState(outState);
	}
	
}
