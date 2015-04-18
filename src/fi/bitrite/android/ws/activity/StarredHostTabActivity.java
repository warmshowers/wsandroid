package fi.bitrite.android.ws.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.gms.analytics.GoogleAnalytics;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;
import java.util.List;

public class StarredHostTabActivity extends WSBaseActivity
        implements android.widget.AdapterView.OnItemClickListener {

    private static final int CONTEXT_MENU_UPDATE = 0;
    private static final int CONTEXT_MENU_DELETE = 1;

    ListView starredHostList;
    TextView noStarredHostsLabel;

    private StarredHostDao starredHostDao = new StarredHostDaoImpl();
    private List<HostBriefInfo> starredHosts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.starred_hosts_tab);
        initView();

        starredHostList = (ListView)findViewById(R.id.lstStarredHosts);
        noStarredHostsLabel = (TextView)findViewById(R.id.lblNoStarredHosts);

        registerForContextMenu(starredHostList);
    }

    private void setupStarredHostsList() {
        starredHosts = starredHostDao.getAllBrief();

        if (starredHosts.size() == 0) {
            noStarredHostsLabel.setVisibility(View.VISIBLE);
            starredHostList.setVisibility(View.GONE);
        } else {
            noStarredHostsLabel.setVisibility(View.GONE);
            starredHostList.setVisibility(View.VISIBLE);

            starredHostList.setAdapter(new HostListAdapter(this, R.layout.host_list_item, starredHosts));

            starredHostList.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent i = new Intent(StarredHostTabActivity.this, HostInformationActivity.class);
                    HostBriefInfo selectedHost = starredHosts.get(position);
                    i.putExtra("id", selectedHost.getId());
                    i.putExtra("host", Host.createFromBriefInfo(selectedHost));
                    startActivityForResult(i, 0);
                }
            });
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        if (view.getId() == starredHostList.getId()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(starredHosts.get(info.position).getFullname());
            menu.add(Menu.NONE, CONTEXT_MENU_UPDATE, 0, R.string.update);
            menu.add(Menu.NONE, CONTEXT_MENU_DELETE, 1, R.string.delete);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int index = item.getItemId();
        switch (index) {
            case CONTEXT_MENU_UPDATE:
                Intent i = new Intent(StarredHostTabActivity.this, HostInformationActivity.class);
                HostBriefInfo selectedHost = starredHosts.get(info.position);
                i.putExtra("id", selectedHost.getId());
                i.putExtra("host", Host.createFromBriefInfo(selectedHost));
                i.putExtra("update", true);
                startActivityForResult(i, 0);
                break;
            case CONTEXT_MENU_DELETE:
                HostBriefInfo host = starredHosts.get(info.position);
                starredHostDao.delete(host.getId(), host.getName());
                setupStarredHostsList();
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        starredHostDao.open();
        setupStarredHostsList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        starredHostDao.close();
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }
}
