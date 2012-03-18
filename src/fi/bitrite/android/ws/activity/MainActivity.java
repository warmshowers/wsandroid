package fi.bitrite.android.ws.activity;

import roboguice.activity.RoboTabActivity;
import android.accounts.Account;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;

public class MainActivity extends RoboTabActivity  {
	
	private Dialog splashDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setupTabs();
		setupCredentials();
		
		if (savedInstanceState != null) {
			boolean splash = savedInstanceState.getBoolean("splash", false);
			if (splash) {
				showAboutDialog();
			}
		}
	}

	private void setupCredentials() {
		try {
			AuthenticationHelper.getWarmshowersAccount();
		}
		
		catch (NoAccountException e) {
			startAuthenticatorActivity(new Intent(MainActivity.this, AuthenticatorActivity.class));
		}
	}
	
	private void startAuthenticatorActivity(Intent i) {
		i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		overridePendingTransition(0, 0);
		startActivityForResult(i, 0);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (initialAccountCreation(intent)) {
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(getApplicationContext(), R.string.need_account, Toast.LENGTH_LONG).show();
				finishWithDelay();
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

	private void finishWithDelay() {
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (Exception e) {
				}
				finish();
			}
		}).start();
	}

	private void setupTabs() {
		TabHost tabHost = getTabHost();
		addTab(tabHost, "tab_starred", R.drawable.tab_icon_starred, new Intent(this, StarredHostTabActivity.class));
		addTab(tabHost, "tab_list", R.drawable.tab_icon_list, new Intent(this, ListSearchTabActivity.class));
		addTab(tabHost, "tab_map", R.drawable.tab_icon_map, new Intent(this, MapSearchTabActivity.class));
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
		case R.id.menuAbout:
			showAboutDialog();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void startAuthenticationActivityForExistingAccount() {
		Intent i = new Intent(MainActivity.this, AuthenticatorActivity.class);
		Account account = AuthenticationHelper.getWarmshowersAccount();
		i.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
		startAuthenticatorActivity(i);
	}

	private void showAboutDialog() {
	    splashDialog = new Dialog(this, R.style.about_dialog);
	    splashDialog.setContentView(R.layout.about);
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
}
