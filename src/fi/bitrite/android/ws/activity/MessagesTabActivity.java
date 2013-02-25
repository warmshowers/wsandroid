package fi.bitrite.android.ws.activity;

import android.os.Bundle;
import fi.bitrite.android.ws.R;
import roboguice.activity.RoboActivity;

public class MessagesTabActivity extends RoboActivity {

	private DialogHandler dialogHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.messages_tab);
		dialogHandler = new DialogHandler(this);
	}

}
