package fi.bitrite.android.ws.activity;

import android.accounts.Account;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
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
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.ArrayList;
import java.util.HashMap;

import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.model.NavRow;

abstract class WSBaseActivity extends ActionBarActivity implements android.widget.AdapterView.OnItemClickListener {
    protected Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mLeftDrawerList;
    private NavDrawerListAdapter mNavDrawerListAdapter;

    public static final String TAG = "WSBaseActivity";
    private Dialog splashDialog;
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
//            mActivityNameMap.put(mNavMenuActivities[i], i);
//            mActivityClassToFriendly.put(mNavMenuActivities[i], mNavMenuOptions[i]);
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

//        ListView leftDrawer = (ListView)findViewById(R.id.left_drawer);
//        View navItem = (View)leftDrawer.getItemAtPosition(mActivityNameMap.get(mActivityName));


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
        switch (item.getItemId()) {
            case R.id.menuAccount:
                startAuthenticationActivityForExistingAccount();
                return true;
            case R.id.menuSettings:
                startSettingsActivity();
                return true;
            case R.id.menuAbout:
                showAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    private void startAuthenticatorActivity(Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
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
            startAuthenticatorActivity(new Intent(this, AuthenticatorActivity.class));
            return false; // Wait to set up tabs until auth is done
        }
    }

    private void startAuthenticationActivityForExistingAccount() {
        Intent i = new Intent(this, AuthenticatorActivity.class);
        try {
            Account account = AuthenticationHelper.getWarmshowersAccount();
            i.putExtra("username", account.name);
        } catch (NoAccountException e) {
            // We have no account, so forget it.
        }
        startAuthenticatorActivity(i);
    }
    private void startSettingsActivity() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    private void showAboutDialog() {
        splashDialog = new Dialog(this, R.style.about_dialog);
        splashDialog.setContentView(R.layout.about);
        TextView versionTextView = (TextView)splashDialog.findViewById(R.id.app_version);
        versionTextView.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));
        TextView googleDetails = (TextView)splashDialog.findViewById(R.id.txtAboutDetailsGoogle);
        String licenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);
        if (licenseInfo != null) {
            // licenseInfo is a bit of a mess (coming directly from google)
            // Change the multi-\n to <br/>, then change single \n perhaps followed by whitespace to a space
            // then change the <br/> back to \n
            licenseInfo = licenseInfo.replaceAll("\n\n+", "<br/>").replaceAll("\n[ \t]*", " ").replace("<br/>", "\n");
            googleDetails.setText(licenseInfo);
        }
        splashDialog.show();
    }

}