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
import android.widget.Toast;

import com.google.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.activity.dialog.SearchDialogHandler;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.search.Search;
import fi.bitrite.android.ws.search.SearchFactory;
import fi.bitrite.android.ws.search.SearchThread;

public class MainActivity extends RoboTabActivity  {
	// Starred hosts tab
	@InjectView(R.id.starredHostsTab) LinearLayout starredHostsTab;
	@InjectView(R.id.lstStarredHosts) ListView starredHostsList;
	
	// List search tab
	@InjectView(R.id.listTab) LinearLayout listTab;
	@InjectView(R.id.editListSearch) EditText listSearchEdit;
	@InjectView(R.id.btnListSearch) ImageView listSearchButton;
	@InjectView(R.id.lstSearchResult) ListView listSearchResult;

	// Utilities
	@Inject StarredHostDao starredHostDao;
	@Inject SearchFactory searchFactory;

	SearchDialogHandler searchDialogHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		setupTabs();

		setupCredentials();
		
		setupStarredHostsList();
		setupListSearch();
		
		searchDialogHandler = new SearchDialogHandler(this);
	}

	private void setupCredentials() {
		try {
			AuthenticationHelper.getWarmshowersAccount();
		}
		
		catch (NoAccountException e) {
			Intent i = new Intent(MainActivity.this, AuthenticatorActivity.class);
			startActivityForResult(i, 0);
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_CANCELED) {
			Toast.makeText(getApplicationContext(), "Cannot run without WarmShowers credentials", Toast.LENGTH_LONG).show();
			finishWithDelay();
		}
	}
	
	private void finishWithDelay() {
		new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(3000);
                }
                catch (Exception e) { }
                finish();
            }
        }).start();
	}	

	private void setupTabs() {
		TabHost tabHost = this.getTabHost();
		addTab(tabHost, "tab_starred", "Starred", starredHostsTab.getId());
		addTab(tabHost, "tab_list", "List", listTab.getId());
		addTab(tabHost, "tab_map", "Map", new Intent(getApplicationContext(), MapTabActivity.class));
	}

	private void addTab(TabHost tabHost, String tabSpec, String indicator, int content) {
		tabHost.addTab(tabHost.newTabSpec(tabSpec).setIndicator(indicator).setContent(content));
	}
	
	private void addTab(TabHost tabHost, String tabSpec, String indicator, Intent content) {
		tabHost.addTab(tabHost.newTabSpec(tabSpec).setIndicator(indicator).setContent(content));
	}

	private void setupStarredHostsList() {
		List<HostBriefInfo> starredHosts = starredHostDao.getAllBrief();
		starredHostsList.setAdapter(new HostListAdapter(this, R.layout.host_list_item, starredHosts));

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
				searchDialogHandler.prepareSearch(SearchDialogHandler.TEXT_SEARCH);
				searchDialogHandler.doSearch();
			}
		});

		listSearchResult.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent i = new Intent(MainActivity.this, HostInformationActivity.class);
				HostBriefInfo briefInfo = (HostBriefInfo) listSearchResult.getItemAtPosition(position);
				Host host = Host.createFromBriefInfo(briefInfo);
				i.putExtra("host", host);
				startActivity(i);
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		return searchDialogHandler.createDialog(id);
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
			searchDialogHandler.dismiss();

			Object obj = msg.obj;
			
			if (obj instanceof Exception) {
				searchDialogHandler.alertError();
				return;
			}
			
			List<HostBriefInfo> hosts = (List<HostBriefInfo>) obj;
			
			if (hosts.isEmpty()) {
				searchDialogHandler.alertNoResults();
				return;
			}

			listSearchResult.setAdapter(new HostListAdapter(WSAndroidApplication.getAppContext(),
					R.layout.host_list_item, hosts));
		}
	};

}
