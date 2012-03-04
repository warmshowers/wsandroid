package fi.bitrite.android.ws.activity;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.search.impl.HttpHostInformation;

public class HostInformationActivity extends RoboActivity {

	private static final int NO_ID = 0;

	@InjectView(R.id.layoutHostDetails)
	LinearLayout hostDetails;

	@InjectView(R.id.txtHostFullname) TextView fullname;
	@InjectView(R.id.txtHostComments) TextView comments;
	@InjectView(R.id.txtHostLocation) TextView location;
	@InjectView(R.id.txtHostMobilePhone) TextView mobilePhone;
	@InjectView(R.id.txtHostHomePhone) TextView homePhone;
	@InjectView(R.id.txtHostWorkPhone) TextView workPhone;
	@InjectView(R.id.txtPreferredNotice) TextView preferredNotice;
	@InjectView(R.id.txtMaxGuests) TextView maxGuests;
	@InjectView(R.id.txtNearestAccomodation) TextView nearestAccomodation;
	@InjectView(R.id.txtCampground) TextView campground;
	@InjectView(R.id.txtBikeShop) TextView bikeShop;
	@InjectView(R.id.txtServices) TextView services;

	@Inject HttpAuthenticationService authenticationService;
	@Inject HttpSessionContainer sessionContainer;

	DialogHandler dialogHandler;
	int id;
	Host host;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host_information);

		Intent i = getIntent();
		host = (Host) i.getParcelableExtra("host");
		id = i.getIntExtra("host_id", NO_ID);

		fullname.setText(host.getFullname());

		dialogHandler = new DialogHandler(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// TODO: no need to download information if user is starred
		dialogHandler.showDialog(DialogHandler.HOST_INFORMATION);
		getHostInformation();
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		return dialogHandler.createDialog(id, "Retrieving host information ...");
	}

	private void getHostInformation() {
		new HostInformationThread(handler, id).start();
	}

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			dialogHandler.dismiss();

			Object obj = msg.obj;

			if (obj instanceof Exception) {
				dialogHandler
						.alertError("Could not retrieve host information. Check your credentials and internet connection.");
				return;
			}

			host = (Host) obj;

			comments.setText(host.getComments());
			location.setText(host.getLocation());
			mobilePhone.setText(host.getMobilePhone());
			homePhone.setText(host.getHomePhone());
			workPhone.setText(host.getWorkPhone());
			preferredNotice.setText(host.getPreferredNotice());
			maxGuests.setText(host.getMaxCyclists());
			nearestAccomodation.setText(host.getMotel());
			campground.setText(host.getCampground());
			bikeShop.setText(host.getBikeshop());
			services.setText(host.getServices());

			hostDetails.setVisibility(View.VISIBLE);

			if (host.isNotCurrentlyAvailable()) {
				dialogHandler
						.alertError("It is indicated that this host is currently not available. You may still contact the host, but you may not get a response.");
			}
		}
	};

	private class HostInformationThread extends Thread {
		Handler handler;
		int id;

		public HostInformationThread(Handler handler, int id) {
			this.handler = handler;
			this.id = id;
		}

		public void run() {
			Message msg = handler.obtainMessage();

			try {
				if (id == NO_ID) {
					// we only have the username, so get the id using that
				}

				HttpHostInformation hostInfo = new HttpHostInformation(id, authenticationService, sessionContainer);
				msg.obj = hostInfo.getHostInformation();
			}

			catch (Exception e) {
				Log.e("WSAndroid", e.getMessage(), e);
				msg.obj = e;
			}

			handler.sendMessage(msg);
		}
	}

}
