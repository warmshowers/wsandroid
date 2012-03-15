package fi.bitrite.android.ws.activity;

import java.util.List;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.persistence.StarredHostDao;

public class StarredHostTabActivity extends RoboActivity {

	@InjectView(R.id.starredHostsTab) LinearLayout starredHostsTab;
	@InjectView(R.id.lstStarredHosts) ListView starredHostsList;
	@InjectView(R.id.lblNoStarredHosts) TextView noStarredHostsLabel;
	
	@Inject StarredHostDao starredHostDao;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.starred_hosts_tab);
		setupStarredHostsList();
	}
		
	private void setupStarredHostsList() {
		List<HostBriefInfo> starredHosts = starredHostDao.getAll();

		if (starredHosts.size() == 0) {
			noStarredHostsLabel.setVisibility(View.VISIBLE);
			starredHostsList.setVisibility(View.GONE);
		} else {
			noStarredHostsLabel.setVisibility(View.GONE);
			starredHostsList.setVisibility(View.VISIBLE);
			
			starredHostsList.setAdapter(new HostListAdapter(this, R.layout.host_list_item, starredHosts));
	
			starredHostsList.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Intent i = new Intent(StarredHostTabActivity.this, HostInformationActivity.class);
					i.putExtra("host", starredHostDao.get(1));
					startActivity(i);
				}
			});
		}
	}
}
