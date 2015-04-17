package fi.bitrite.android.ws.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.widget.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import fi.bitrite.android.ws.R;

abstract class WSBaseActivity extends ActionBarActivity implements android.widget.AdapterView.OnItemClickListener {
    protected Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mLeftDrawerList;
    private ArrayAdapter<String> mNavigationDrawerAdapter;
    protected String[] mNavMenuOptions;

    public static final String TAG = "WSBaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNavMenuOptions = getResources().getStringArray(R.array.navigation_menu);

        initView();
        if (mToolbar != null) {
            mToolbar.setTitle(""); // Garbage text; seems to be required
            setSupportActionBar(mToolbar);
        }
        initDrawer();
    }

    private void initView() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mLeftDrawerList = (ListView) mDrawerLayout.findViewById(R.id.left_drawer);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mNavigationDrawerAdapter = new ArrayAdapter<String>(WSBaseActivity.this, android.R.layout.simple_list_item_1, mNavMenuOptions);
        mLeftDrawerList.setAdapter(mNavigationDrawerAdapter);
        mLeftDrawerList.setOnItemClickListener(this);
    }

    private void initDrawer() {

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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected() fired");
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        String[] activities = getResources().getStringArray(R.array.navigation_menu_activities);
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


}