package fi.bitrite.android.ws.activity;

import fi.bitrite.android.ws.BuildConfig;
import roboguice.activity.RoboTabActivity;
import android.accounts.Account;
import android.accounts.AccountManager;
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

public class MainActivity extends RoboTabActivity  {
    
    private Dialog splashDialog;

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

        handleFirstRun();

        setContentView(R.layout.main);
        if (setupCredentials()) {
            setupTabs();  // If creds already there, set up tabs now, else wait until login
        }

        if (savedInstanceState != null) {
            boolean splash = savedInstanceState.getBoolean("splash", false);
            if (splash) {
                showAboutDialog();
            }
        }
    }
    
    /**
     * New for version code 10. We want to store some additional data with the 
     * account, so we need to remove the old one.
     */
    private void handleFirstRun() {
        boolean firstrun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("v10update", true);
        if (firstrun) {
            try {
                AccountManager accountManager = AccountManager.get(this);
                Account oldAccount = AuthenticationHelper.getWarmshowersAccount();
                accountManager.removeAccount(oldAccount, null, null);
            }
            
            catch (NoAccountException e) {
                // OK, so there was no account - fine
            }
            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("v10update", false).commit();            
        }
    }

    /**
     *
     * @return
     *   true if we already have an account set up and it's working
     *   false if we have to wait for the auth screen to process
     */
    private boolean setupCredentials() {
        try {
            AuthenticationHelper.getWarmshowersAccount();
            return true;
        }
        catch (NoAccountException e) {
            startAuthenticatorActivity(new Intent(MainActivity.this, AuthenticatorActivity.class));
            return false; // Wait to set up tabs until auth is done
        }
    }
    
    private void startAuthenticatorActivity(Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivityForResult(i, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == AuthenticatorActivity.RESULT_NO_NETWORK) {
            Toast.makeText(this, R.string.io_error, Toast.LENGTH_LONG).show();
            return;
        }

        if (resultCode == AuthenticatorActivity.RESULT_OK) {
            setupTabs();
            return;
        }

        if (initialAccountCreation(intent)) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), R.string.need_account, Toast.LENGTH_LONG).show();
                finish();
            } else if (resultCode == AuthenticatorActivity.RESULT_AUTHENTICATION_FAILED) {
                Toast.makeText(getApplicationContext(), R.string.authentication_failed, Toast.LENGTH_LONG).show();
                startAuthenticatorActivity(new Intent(MainActivity.this, AuthenticatorActivity.class));
            }
        } else {
            if (resultCode == AuthenticatorActivity.RESULT_AUTHENTICATION_FAILED) {
                Toast.makeText(getApplicationContext(), R.string.authentication_failed, Toast.LENGTH_LONG).show();
                startAuthenticationActivityForExistingAccount();
            }
        }
    }
        
    private boolean initialAccountCreation(Intent intent) {
        return intent.getBooleanExtra(AuthenticatorActivity.PARAM_INITIAL_AUTHENTICATION, true);
    }


    private void setupTabs() {
        if (tabsCreated) {
            return;
        }
        TabHost tabHost = getTabHost();
        addTab(tabHost, "tab_map", R.drawable.tab_icon_map, new Intent(this, Maps2Activity.class));
        addTab(tabHost, "tab_list", R.drawable.tab_icon_list, new Intent(this, ListSearchTabActivity.class));
        addTab(tabHost, "tab_starred", R.drawable.tab_icon_starred, new Intent(this, StarredHostTabActivity.class));
        addTab(tabHost, "tab_messages", R.drawable.tab_icon_messages, new Intent(this, MessagesTabActivity.class));
        tabsCreated = true;
    }

    private void addTab(TabHost tabHost, String tabSpec, int icon, Intent content) {
        tabHost.addTab(tabHost.newTabSpec(tabSpec).setIndicator("", getResources().getDrawable(icon)).setContent(content));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
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

    private void startAuthenticationActivityForExistingAccount() {
        Intent i = new Intent(MainActivity.this, AuthenticatorActivity.class);
        try {
            Account account = AuthenticationHelper.getWarmshowersAccount();
            i.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
        } catch (NoAccountException e) {
            // We have no account, so forget it.
        }
        startAuthenticatorActivity(i);
    }

    private void startSettingsActivity() {
        Intent i = new Intent(MainActivity.this, SettingsActivity.class);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (splashDialog != null && splashDialog.isShowing()) {
            outState.putBoolean("splash", true);
        }
    }

    public void switchTab(int tab) {
        getTabHost().setCurrentTab(tab);
    }
    
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
