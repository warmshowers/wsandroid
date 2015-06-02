package fi.bitrite.android.ws.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.HashMap;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.NavRow;
import fi.bitrite.android.ws.util.MemberInfo;

abstract class WSBaseActivity extends AppCompatActivity implements android.widget.AdapterView.OnItemClickListener {
    protected Toolbar mToolbar;
    protected DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mLeftDrawerList;
    private NavDrawerListAdapter mNavDrawerListAdapter;
    private int mCurrentActivity;

    public static final String TAG = "WSBaseActivity";
    protected String mActivityName = this.getClass().getSimpleName();
    protected ArrayList<NavRow> mNavRowList = new ArrayList<NavRow>();
    String mActivityFriendly;

    protected boolean mHasBackIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] navMenuOptions = getResources().getStringArray(R.array.nav_menu_options);
        String[] navMenuActivities = getResources().getStringArray(R.array.nav_menu_activities);
        HashMap<String, String> mActivityClassToFriendly = new HashMap<String, String>();

        TypedArray icons = getResources().obtainTypedArray(R.array.nav_menu_icons);
        for (int i = 0; i < navMenuOptions.length; i++) {
            mActivityClassToFriendly.put(navMenuActivities[i], navMenuOptions[i]);

            int icon = icons.getResourceId(i, R.drawable.ic_action_email);
            NavRow row = new NavRow(icon, navMenuOptions[i], navMenuActivities[i]);
            mNavRowList.add(row);

            if (navMenuActivities[i].equals(mActivityName)) {
                mCurrentActivity = i;
            }
        }
        mActivityFriendly = mActivityClassToFriendly.get(mActivityName);
    }

    protected void initView() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mLeftDrawerList = (ListView) mDrawerLayout.findViewById(R.id.left_drawer);
        mLeftDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mNavDrawerListAdapter = new NavDrawerListAdapter(this, mNavRowList, mCurrentActivity);
        mLeftDrawerList.setAdapter(mNavDrawerListAdapter);
        mLeftDrawerList.setOnItemClickListener(this);

        if (mToolbar != null) {
            mToolbar.setTitle(mActivityFriendly);
            setSupportActionBar(mToolbar);
        }

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

        initDrawer();
    }

    protected void initDrawer() {

        final TextView lblUsername = (TextView) mDrawerLayout.findViewById(R.id.lblUsername);
        final TextView lblNotLoggedIn = (TextView) mDrawerLayout.findViewById(R.id.lblNotLoggedIn);
        final TextView lblFullname = (TextView) mDrawerLayout.findViewById(R.id.lblFullname);
        final ImageView memberPhoto = (ImageView) mDrawerLayout.findViewById(R.id.imgUserPhoto);


        Host memberInfo = MemberInfo.getMemberInfo();
        if (memberInfo != null) {
            lblUsername.setText(memberInfo.getName());
            lblFullname.setText(memberInfo.getFullname());
            String photoPath = MemberInfo.getMemberPhotoFilePath();
            if (photoPath != null && memberPhoto != null) {
                memberPhoto.setImageBitmap(BitmapFactory.decodeFile(photoPath));
            } else {
                memberPhoto.setImageResource(R.drawable.default_hostinfo_profile);
            }
        } else {
            memberPhoto.setImageResource(R.drawable.default_hostinfo_profile);
            lblNotLoggedIn.setVisibility(View.VISIBLE);
            lblUsername.setVisibility(View.GONE);
            lblFullname.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mDrawerToggle.syncState();

        if (mHasBackIntent) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
            mDrawerLayout.closeDrawers();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!setupCredentials()) {
            return;
        }

        initDrawer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Update the drawer if we're returning from another activity
        initDrawer();
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
        if (mActivityName.equals(activities[position])) return;

        try {
            Class activityClass = Class.forName(this.getPackageName() + ".activity." + activities[position]);
            Intent i = new Intent(this, activityClass);
            startActivity(i);
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "Class not found: " + activities[position]);
        }

        mDrawerLayout.closeDrawers();
    }


    private void startAuthenticatorActivity(Intent i) {
        startActivityForResult(i, AuthenticatorActivity.REQUEST_TYPE_AUTHENTICATE);
    }

    /**
     * @return true if we already have an account set up in the AccountManager
     * false if we have to wait for the auth screen to process
     */
    public boolean setupCredentials() {
        try {
            AuthenticationHelper.getWarmshowersAccount();
            if (MemberInfo.getInstance() == null) {
                MemberInfo.initInstance(null); // Try to get persisted information
            }
            return true;
        } catch (NoAccountException e) {

            if (this.getClass() != AuthenticatorActivity.class) {  // Would be circular, so don't do it.
                startAuthenticatorActivity(new Intent(this, AuthenticatorActivity.class));
            }
            return false;
        }
    }

}