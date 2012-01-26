package fi.bitrite.android.ws.activity;

import roboguice.activity.RoboActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;

public class HostInformationActivity extends RoboActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host_information);
		
		Intent i = getIntent();
		Host host = (Host) i.getParcelableExtra("host");
		
		Log.d("HostActivity", host.getFullname());
	}

}
