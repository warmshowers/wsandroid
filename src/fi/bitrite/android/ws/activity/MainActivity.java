package fi.bitrite.android.ws.activity;

import java.util.List;

import roboguice.activity.RoboTabActivity;
import roboguice.inject.InjectView;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.search.SearchFactory;

public class MainActivity extends RoboTabActivity {
	private static final int PROGRESS_DIALOG_TEXT_SEARCH = 0;
	
	@InjectView(R.id.starredHostsTab) LinearLayout starredHostsTab;
	@InjectView(R.id.listTab) LinearLayout listTab;
	@InjectView(R.id.mapTab) LinearLayout mapTab;

	@InjectView(R.id.lstStarredHosts) ListView starredHostsList;
	@InjectView(R.id.editListSearch) EditText listSearchEdit;
	@InjectView(R.id.btnListSearch) ImageView listSearchButton;
	
	@Inject StarredHostDao starredHostDao;
	
	@Inject SearchFactory searchFactory;
	
	SearchThread searchThread;
	ProgressDialog progressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		setupTabs();
		
		setupStarredHostsList();
		setupListSearch();
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
        starredHostsList.setAdapter(new StarredHostsAdapter(this, R.layout.starred_hosts_item, starredHosts));

        // starredHostsList.setTextFilterEnabled(true);   // adapter needs to implement filterable for this

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
				showDialog(PROGRESS_DIALOG_TEXT_SEARCH);
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		progressDialog = new ProgressDialog(MainActivity.this);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage("Performing search ...");
		return progressDialog;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case PROGRESS_DIALOG_TEXT_SEARCH:
			String text = listSearchEdit.getText().toString();
			searchThread = new SearchThread(handler, searchFactory.createTextSearch(text));
			searchThread.start();
		}
	}

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO: 
			// - error handling (msg contains error code?)
			// - display results
			dismissDialog(PROGRESS_DIALOG_TEXT_SEARCH);
		}
	};

}
