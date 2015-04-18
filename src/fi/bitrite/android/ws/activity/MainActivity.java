package fi.bitrite.android.ws.activity;

import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.auth.Authenticator;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.common.GooglePlayServicesUtil;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;

import java.util.ArrayList;

public class MainActivity extends Activity {

    static public MainActivity mainActivity;


    // a host is "stashed" when moving from e.g. the host information activity directly
    // to the map ("Show host on map") and back again.
    private Parcelable stashedHost;
    private int stashedHostId;
    private ArrayList<Parcelable> stashedFeedback;
    private static final String TAG = "MainActivity";
    private boolean tabsCreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = this;

        handleFirstRun();

        setContentView(R.layout.main);
//        if (setupCredentials()) {
//            setupTabs();  // If creds already there, set up tabs now, else wait until login
//        }
//
//        if (savedInstanceState != null) {
//            boolean splash = savedInstanceState.getBoolean("splash", false);
//            if (splash) {
//                showAboutDialog();
//            }
//        }
    }
    
    /**
     * New for version code 10. We want to store some additional data with the 
     * account, so we need to remove the old one.
     */
    private void handleFirstRun() {
        boolean firstrun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("v10update", true);
        if (firstrun) {
            AuthenticationHelper.removeOldAccount();
            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("v10update", false).commit();
        }
    }


    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // We don't have a reason here to handle anything that's not a result of authentication request
        if (requestCode != AuthenticatorActivity.REQUEST_TYPE_AUTHENTICATE || intent == null) {
            return;
        }
        if (resultCode == AuthenticatorActivity.RESULT_NO_NETWORK) {
            Toast.makeText(this, R.string.io_error, Toast.LENGTH_LONG).show();
            finish();
        } else if (resultCode == AuthenticatorActivity.RESULT_OK) {
//            setupTabs();
            return;
        } else if (resultCode == AuthenticatorActivity.RESULT_CANCELED) {
            try {
                Account account = AuthenticationHelper.getWarmshowersAccount();
            } catch (NoAccountException e) {
                Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

//    private void setupTabs() {
//        if (tabsCreated) {
//            return;
//        }
//        TabHost tabHost = getTabHost();
//        addTab(tabHost, "tab_map", R.drawable.tab_icon_map, new Intent(this, Maps2Activity.class));
//        addTab(tabHost, "tab_list", R.drawable.tab_icon_list, new Intent(this, ListSearchTabActivity.class));
//        addTab(tabHost, "tab_starred", R.drawable.tab_icon_starred, new Intent(this, StarredHostTabActivity.class));
//        addTab(tabHost, "tab_messages", R.drawable.tab_icon_messages, new Intent(this, MessagesTabActivity.class));
//        tabsCreated = true;
//    }
//
//    private void addTab(TabHost tabHost, String tabSpec, int icon, Intent content) {
//        tabHost.addTab(tabHost.newTabSpec(tabSpec).setIndicator("", getResources().getDrawable(icon)).setContent(content));
//    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }   
    
    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//        case R.id.menuAccount:
//            startAuthenticationActivityForExistingAccount();
//            return true;
//        case R.id.menuSettings:
//            startSettingsActivity();
//            return true;
//        case R.id.menuAbout:
//            showAboutDialog();
//            return true;
//        default:
//            return super.onOptionsItemSelected(item);
//        }
//    }



    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        if (splashDialog != null && splashDialog.isShowing()) {
//            outState.putBoolean("splash", true);
//        }
    }

//    public void switchTab(int tab) {
//        getTabHost().setCurrentTab(tab);
//    }
    
    public void stashHost(Intent data, int stashedFrom) {
        stashedHost = data.getParcelableExtra("host");
        stashedHostId = data.getIntExtra("id", 0);
        stashedFeedback = data.getParcelableArrayListExtra("feedback");
    }

    public boolean hasStashedHost() {
        return stashedHost != null;
    }
    
    public Intent popStashedHost(Intent i) {
        i.putExtra("host", stashedHost);
        i.putExtra("id", stashedHostId);
        i.putExtra("feedback", stashedFeedback);
        i.putExtra("full_info", true);
        stashedHost = null;
        stashedHostId = 0;
        return i;
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
