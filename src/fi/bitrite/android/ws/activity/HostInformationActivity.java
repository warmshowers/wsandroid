package fi.bitrite.android.ws.activity;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.search.impl.HttpHostInformation;

public class HostInformationActivity extends RoboActivity {

	private static final int NO_ID = 0;

	@InjectView(R.id.layoutHostDetails)
	LinearLayout hostDetails;

	@InjectView(R.id.btnHostStar) ImageView star;
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

	@Inject StarredHostDao starredHostDao;

	private DialogHandler dialogHandler;
	
	private Host host;
	private int id;
	private boolean starred;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host_information);
		dialogHandler = new DialogHandler(this);

		// TODO: no need to download information if user is starred
		//  -> should check if host information is "complete" somehow. 
		
		if (savedInstanceState != null) {
			host = savedInstanceState.getParcelable("host");
			id = savedInstanceState.getInt("host_id");
			
			// TODO: we can only set the view content if host is complete!
			// otherwise we have to download it again.
			// this is a problem when changing screen rotation during
			// loading of host.
			setViewContentFromHost();
		} else {
			Intent i = getIntent();
			host = (Host) i.getParcelableExtra("host");
			id = i.getIntExtra("host_id", NO_ID);

			dialogHandler.showDialog(DialogHandler.HOST_INFORMATION);
			getHostInformationAsync();
		}
		
		fullname.setText(host.getFullname());
		starred = starredHostDao.isHostStarred(id, host.getName());
		setupStar();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO: save dialog state?
		outState.putParcelable("host", host);
		outState.putInt("host_id", id);
		super.onSaveInstanceState(outState);
	}

	private void setupStar() {
		int drawable = starred ? R.drawable.starred_on : R.drawable.starred_off;
		star.setImageDrawable(getResources().getDrawable(drawable));
		star.setVisibility(View.VISIBLE);
	}

	public void showStarHostDialog(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(starred ? "Un-star this host?" : "Star this host?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                HostInformationActivity.this.toggleHostStarred();
		                dialog.dismiss();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	protected void toggleHostStarred() {
		starred = !starred;
		setupStar();
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		return dialogHandler.createDialog(id, "Retrieving host information ...");
	}

	private void getHostInformationAsync() {
		new HostInformationThread(handler, id).start();
	}

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			dialogHandler.dismiss();

			Object obj = msg.obj;

			if (obj instanceof Exception) {
				dialogHandler.alertError(
						"Could not retrieve host information. Check your credentials and internet connection.");
				return;
			}

			host = (Host) obj;

			setViewContentFromHost();

			if (host.isNotCurrentlyAvailable()) {
				dialogHandler
						.alertError(
								"It is indicated that this host is currently not available. You may still contact the host, but you may not get a response.");
			}
		}

	};

	private void setViewContentFromHost() {
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
	}

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
				HttpHostInformation hostInfo = new HttpHostInformation(authenticationService, sessionContainer);

				if (id == NO_ID) {
					id = hostInfo.getHostId(host.getName());
				}

				msg.obj = hostInfo.getHostInformation(id);
			}

			catch (Exception e) {
				Log.e("WSAndroid", e.getMessage(), e);
				msg.obj = e;
			}

			handler.sendMessage(msg);
		}
	}

}
