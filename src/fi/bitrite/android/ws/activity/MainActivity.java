package fi.bitrite.android.ws.activity;

import java.util.List;

import roboguice.activity.RoboTabActivity;
import roboguice.inject.InjectView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;

public class MainActivity extends RoboTabActivity {
	
	@InjectView(R.id.lstStarredHosts) ListView starredHostsList;
	@InjectView(R.id.txtList) TextView listPlaceholder;
	@InjectView(R.id.txtMap) TextView mapPlaceholder;

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
		addTab(tabHost, "tab_starred", "Starred", starredHostsList.getId());
		addTab(tabHost, "tab_list", "List", listPlaceholder.getId());
		addTab(tabHost, "tab_map", "Map", mapPlaceholder.getId());
	}

	private void setupStarredHostsList() {
        List<Host> starredHosts = starredHostDao.getStarredHosts();
        starredHostsList.setAdapter(new StarredHostsAdapter(this, R.layout.starred_hosts_item, starredHosts));

        // starredHostsList.setTextFilterEnabled(true);   // adapter needs to implement filterable for this

        starredHostsList.setOnItemClickListener(new OnItemClickListener() {
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	  Intent i = new Intent(MainActivity.this, HostInformationActivity.class);
        	  i.putExtra("host", starredHostDao.getStarredHost());
        	  startActivity(i);
          }
        });
	}
	
	private void addTab(TabHost tabHost, String tabSpec, String indicator, int content) {
		tabHost.addTab(tabHost.newTabSpec(tabSpec).setIndicator(indicator).setContent(content));
	}
}
