package fi.bitrite.android.ws.activity;

import roboguice.activity.RoboActivity;
import android.os.Bundle;
import fi.bitrite.android.ws.R;

public class HostInformationActivity extends RoboActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host_information);
	}

}
