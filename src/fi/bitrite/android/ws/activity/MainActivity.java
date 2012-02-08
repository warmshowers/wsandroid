package fi.bitrite.android.ws.activity;

import java.util.List;

import roboguice.activity.RoboTabActivity;
import roboguice.inject.InjectView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;

import com.google.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;

public class MainActivity extends RoboTabActivity {
	@InjectView(R.id.starredHostsTab) LinearLayout starredHostsTab;
	@InjectView(R.id.listTab) LinearLayout listTab;
	@InjectView(R.id.mapTab) LinearLayout mapTab;

	@InjectView(R.id.lstStarredHosts) ListView starredHostsList;
	
	@Inject StarredHostDao starredHostDao;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		setupTabs();
		setupStarredHostsList();
	}
	
	private void setupTabs() {
		TabHost tabHost = this.getTabHost();
		addTab(tabHost, "tab_starred", "Starred", starredHostsTab.getId());
		addTab(tabHost, "tab_list", "List", listTab.getId());
		addTab(tabHost, "tab_map", "Map", mapTab.getId());
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
	
	private void addTab(TabHost tabHost, String tabSpec, String indicator, int content) {
		tabHost.addTab(tabHost.newTabSpec(tabSpec).setIndicator(indicator).setContent(content));
	}
}
