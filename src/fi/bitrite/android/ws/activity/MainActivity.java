package fi.bitrite.android.ws.activity;

import java.util.List;

import roboguice.activity.RoboTabActivity;
import roboguice.inject.InjectView;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;

import com.google.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.activity.dialog.SearchDialog;
import fi.bitrite.android.ws.activity.dialog.SearchDialogProvider;
import fi.bitrite.android.ws.auth.AuthenticationService;
import fi.bitrite.android.ws.auth.CredentialsService;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.search.Search;
import fi.bitrite.android.ws.search.SearchFactory;

public class MainActivity extends RoboTabActivity {
	// Starred hosts tab
	@InjectView(R.id.starredHostsTab) LinearLayout starredHostsTab;
	@InjectView(R.id.lstStarredHosts) ListView starredHostsList;
	
	// List search tab
	@InjectView(R.id.listTab) LinearLayout listTab;
	@InjectView(R.id.editListSearch) EditText listSearchEdit;
	@InjectView(R.id.btnListSearch) ImageView listSearchButton;
	@InjectView(R.id.lstSearchResult) ListView listSearchResult;

	// Map tab
	@InjectView(R.id.mapTab) LinearLayout mapTab;
	
	// Utilities
	@Inject StarredHostDao starredHostDao;
	@Inject SearchFactory searchFactory;
	@Inject CredentialsService credentialsService;
	@Inject AuthenticationService authenticationService;

	SearchDialog searchDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		setupTabs();

		setupStarredHostsList();
		setupListSearch();
		
		searchDialog = new SearchDialogProvider(this, credentialsService);
	}

	private void setupTabs() {
		TabHost tabHost = this.getTabHost();
		addTab(tabHost, "tab_starred", "Starred", starredHostsTab.getId());
		addTab(tabHost, "tab_list", "List", listTab.getId());
		addTab(tabHost, "tab_map", "Map", mapTab.getId());
	}

	private void addTab(TabHost tabHost, String tabSpec, String indicator, int content) {
		tabHost.addTab(tabHost.newTabSpec(tabSpec).setIndicator(indicator).setContent(content));
	}

	private void setupStarredHostsList() {
		List<Host> starredHosts = starredHostDao.getAll();
		starredHostsList.setAdapter(new HostListAdapter(this, R.layout.host_list_item, starredHosts));

		// starredHostsList.setTextFilterEnabled(true); // adapter needs to
		// implement filterable for this

		starredHostsList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent i = new Intent(MainActivity.this, HostInformationActivity.class);
				i.putExtra("host", starredHostDao.get());
				startActivity(i);
			}
		});
	}

	private void setupListSearch() {
		listSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchDialog.showTextSearchDialog();
			}
		});

		listSearchResult.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent i = new Intent(MainActivity.this, HostInformationActivity.class);
				i.putExtra("host", starredHostDao.get());
				startActivity(i);
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		return searchDialog.createDialog(id);
	}

	public void doTextSearch() {
		String searchText = listSearchEdit.getText().toString(); 
		Search search = searchFactory.createTextSearch(searchText);
		
		Thread searchThread = new SearchThread(handler, search);
		searchThread.start();
	}

	final Handler handler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			searchDialog.dismiss();

			Object obj = msg.obj;
			
			if (obj instanceof Exception) {
				searchDialog.alertError();
				return;
			}
			
			List<Host> hosts = (List<Host>) obj;
			
			if (hosts.isEmpty()) {
				searchDialog.alertNoResults();
				return;
			}

			listSearchResult.setAdapter(new HostListAdapter(WSAndroidApplication.getAppContext(),
					R.layout.host_list_item, hosts));
		}
	};
}
