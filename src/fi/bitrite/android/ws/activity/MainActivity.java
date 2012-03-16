package fi.bitrite.android.ws.activity;

import roboguice.activity.RoboTabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.auth.NoAccountException;

public class MainActivity extends RoboTabActivity  {
	
	private GeoPoint mapTarget;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setupCredentials();
		setupTabs();
	}

	private void setupCredentials() {
		try {
			AuthenticationHelper.getWarmshowersAccount();
		}
		
		catch (NoAccountException e) {
			startAuthenticatorActivity();
		}
	}
	
	private void startAuthenticatorActivity() {
		Intent i = new Intent(MainActivity.this, AuthenticatorActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		overridePendingTransition(0, 0);
		startActivityForResult(i, 0);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_CANCELED) {
			Toast.makeText(getApplicationContext(), "Cannot run without WarmShowers credentials", Toast.LENGTH_LONG).show();
			finishWithDelay();
		} else if (resultCode == AuthenticatorActivity.RESULT_AUTHENTICATION_FAILED) {
			Toast.makeText(getApplicationContext(), "Authentication failed. Check your credentials and internet connection", Toast.LENGTH_LONG).show();
			startAuthenticatorActivity();
		}
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
	
	public void switchTab(int tab) {
        getTabHost().setCurrentTab(tab);
	}
	
	public void clearMapTarget() {
		mapTarget = null;
	}
	
	public void setMapTarget(GeoPoint target) {
		mapTarget = target;
	}
	
	public GeoPoint getMapTarget() {
		return mapTarget;
	}

}
