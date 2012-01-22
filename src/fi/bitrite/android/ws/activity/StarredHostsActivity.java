package fi.bitrite.android.ws.activity;

import java.util.Arrays;

import roboguice.activity.RoboListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;

public class StarredHostsActivity extends RoboListActivity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Host [] starredHosts = {  new Host("Johannes Staffans", "Nice Host"),  new Host("Johannes Staffans", "Nice Host") };
        setListAdapter(new StarredHostsAdapter(this, R.layout.starred_hosts_item, Arrays.asList(starredHosts)));

        ListView lv = getListView();
        
        // lv.setTextFilterEnabled(true);   // adapter needs to implement filterable for this

        lv.setOnItemClickListener(new OnItemClickListener() {
          public void onItemClick(AdapterView<?> parent, View view,
              int position, long id) {
        	  Intent i = new Intent(StarredHostsActivity.this, HostInformationActivity.class);
        	  startActivity(i);
          }
        });
	}
}