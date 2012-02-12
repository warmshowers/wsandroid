package fi.bitrite.android.ws.activity;

import java.util.List;

import roboguice.activity.RoboTabActivity;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.search.SearchFactory;

public class MainActivity extends RoboTabActivity {
	private static final int PROGRESS_DIALOG_TEXT_SEARCH = 0;

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
				showDialog(PROGRESS_DIALOG_TEXT_SEARCH);
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
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			// TODO:
			// - error handling (msg contains error code?)
			List<Host> hosts = (List<Host>) msg.obj;
			dismissDialog(PROGRESS_DIALOG_TEXT_SEARCH);

			if (hosts.size() == 0) {
				alertNoResults();
			}

			listSearchResult.setAdapter(new HostListAdapter(WSAndroidApplication.getAppContext(),
					R.layout.host_list_item, hosts));
		}
	};

	protected void alertNoResults() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Your search yielded no results.").setCancelable(false)
				.setNeutralButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

}
