package fi.bitrite.android.ws.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import fi.bitrite.android.ws.R;

public class SettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
