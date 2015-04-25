package fi.bitrite.android.ws.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.widget.*;
import android.widget.*;
import java.util.ArrayList;
import java.util.HashMap;

import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.model.NavRow;

abstract class WSBaseActivity extends AppCompatActivity implements android.widget.AdapterView.OnItemClickListener {
    protected Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mLeftDrawerList;
    private NavDrawerListAdapter mNavDrawerListAdapter;

    public static final String TAG = "WSBaseActivity";
    protected String mActivityName = this.getClass().getSimpleName();
    protected ArrayList<NavRow> mNavRowList = new ArrayList<NavRow>();
    String mActivityFriendly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        String[] navMenuOptions = getResources().getStringArray(R.array.nav_menu_options);
        String[] navMeuActivities = getResources().getStringArray(R.array.nav_menu_activities);
        HashMap<String, String> mActivityClassToFriendly = new HashMap<String, String>();

        TypedArray icons = getResources().obtainTypedArray(R.array.nav_menu_icons);
        for (int i=0; i<navMenuOptions.length; i++) {
            mActivityClassToFriendly.put(navMeuActivities[i], navMenuOptions[i]);

            // TODO: Fix the default icon, implement the action management
            int icon = icons.getResourceId(i, R.drawable.ic_action_email);
            NavRow row = new NavRow(icon, navMenuOptions[i], navMeuActivities[i]);
            mNavRowList.add(row);
        }
        mActivityFriendly = mActivityClassToFriendly.get(mActivityName);

    }

    protected void initView() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mLeftDrawerList = (ListView) mDrawerLayout.findViewById(R.id.left_drawer);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mNavDrawerListAdapter = new NavDrawerListAdapter(this, mNavRowList);
        mLeftDrawerList.setAdapter(mNavDrawerListAdapter);
        mLeftDrawerList.setOnItemClickListener(this);

        if (mToolbar != null) {
            mToolbar.setTitle(mActivityFriendly);
            setSupportActionBar(mToolbar);
        }
        initDrawer();
    }

    private void initDrawer() {

        ListView leftDrawer = (ListView)findViewById(R.id.left_drawer);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Make sure we have an active account, or go to authentication screen
        if (!setupCredentials()) {
            return;
        }

        TextView lblUsername = (TextView) mDrawerLayout.findViewById(R.id.lblUsername);
        TextView lblNotLoggedIn = (TextView) mDrawerLayout.findViewById(R.id.lblNotLoggedIn);

        try {
            String username = AuthenticationHelper.getAccountUsername();
            lblUsername.setText(username);
        } catch (NoAccountException e) {
            lblNotLoggedIn.setVisibility(View.VISIBLE);
            lblUsername.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public void onBackPressed() {
        if(mDrawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)){
            mDrawerLayout.closeDrawers();
            return;
        }
        super.onBackPressed();
    }

    /**
     * Handle click from ListView in NavigationDrawer
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String[] activities = getResources().getStringArray(R.array.nav_menu_activities);
        try {
            Class activityClass =  Class.forName(this.getPackageName() + ".activity." + activities[position]);
            Intent i = new Intent(this, activityClass);
            startActivity(i);
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "Class not found: " + activities[position]);
        }
        mDrawerLayout.closeDrawers();
        // Toast.makeText(this, "onItemClick position=" + Integer.toString(position), Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_actions, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        android.widget.SearchView searchView = (android.widget.SearchView) menu.findItem(R.id.action_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default

        return true;
    }


    private void startAuthenticatorActivity(Intent i) {
        startActivityForResult(i, AuthenticatorActivity.REQUEST_TYPE_AUTHENTICATE);
    }

    /**
     *
     * @return
     *   true if we already have an account set up in the AccountManager
     *   false if we have to wait for the auth screen to process
     */
    public boolean setupCredentials() {
        try {
            AuthenticationHelper.getWarmshowersAccount();
            return true;
        }
        catch (NoAccountException e) {

            if (this.getClass() != AuthenticatorActivity.class) {  // Would be circular, so don't do it.
                startAuthenticatorActivity(new Intent(this, AuthenticatorActivity.class));
            }
            return false;
        }
    }

}