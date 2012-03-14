package fi.bitrite.android.ws.activity;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import roboguice.util.Strings;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.inject.Inject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.contact.HttpHostContact;
import fi.bitrite.android.ws.model.Host;

public class HostContactActivity extends RoboActivity {
	
	@InjectView(R.id.txtContactHostTitle) TextView title;
	@InjectView(R.id.editContactHostSubject) EditText editSubject;
	@InjectView(R.id.editContactHostMessage) EditText editMessage;
	
	@Inject HttpAuthenticationService authenticationService;
	@Inject HttpSessionContainer sessionContainer;
	
	private Host host;
	private int id;
	
	private DialogHandler dialogHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host_contact);
		
		dialogHandler = new DialogHandler(this);
		
		if (savedInstanceState != null) {
			host = savedInstanceState.getParcelable("host");
			id = savedInstanceState.getInt("host_id");
		} else {
			Intent i = getIntent();
			host = (Host) i.getParcelableExtra("host");		
			id = i.getIntExtra("id", 0);
		}
		
		title.setText("Message to " + host.getFullname());
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO: save dialog state?
		outState.putParcelable("host", host);
		outState.putInt("host_id", id);
		super.onSaveInstanceState(outState);
	}	
	
	public void sendMessageToHost(View view) {
		String subject = editSubject.getText().toString();
		String message = editMessage.getText().toString();
		
		if (Strings.isEmpty(subject) || Strings.isEmpty(message)) {
			dialogHandler.alert("Both subject and message are obligatory.");
		}
		
		dialogHandler.showDialog(DialogHandler.HOST_CONTACT);
		new HostContactThread(handler, host.getName(), subject, message).start();
	}


	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		return dialogHandler.createDialog(id, "Sending message ...");
	}

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			dialogHandler.dismiss();

			Object obj = msg.obj;

			if (obj instanceof Exception) {
				dialogHandler.alert(
						"Could not send message. Check your credentials and internet connection.");
				return;
			}

			dialogHandler.alert("Message sent successfully.");
		}

	};

	private class HostContactThread extends Thread {
		Handler handler;
		String name;
		String subject;
		String message;

		public HostContactThread(Handler handler, String name, String subject, String message) {
			this.handler = handler;
			this.name = name;
			this.subject = subject;
			this.message = message;
		}

		public void run() {
			Message msg = handler.obtainMessage();

			try {
				new HttpHostContact(authenticationService, sessionContainer).send(name, subject, message);
			}

			catch (Exception e) {
				Log.e("WSAndroid", e.getMessage(), e);
				msg.obj = e;
			}

			handler.sendMessage(msg);
		}
	}	
}
