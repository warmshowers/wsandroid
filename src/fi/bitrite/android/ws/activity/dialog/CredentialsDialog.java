package fi.bitrite.android.ws.activity.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.CredentialsProvider;
import fi.bitrite.android.ws.auth.CredentialsReceiver;

public class CredentialsDialog implements CredentialsProvider {

	Context context;

	String username;

	String password;

	private CredentialsReceiver receiver;

	public CredentialsDialog(Context context, CredentialsReceiver receiver) {
		this.context = context;
		this.receiver = receiver;
	}

	public void show() {
		LayoutInflater factory = LayoutInflater.from(context);
		final View credentialsView = factory.inflate(R.layout.credentials, null);
		Dialog dialog = new AlertDialog.Builder(context).setTitle(R.string.credentials_title).setView(credentialsView)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						EditText usernameView = (EditText) credentialsView.findViewById(R.id.editUsername);
						username = usernameView.getText().toString();
						EditText passwordView = (EditText) credentialsView.findViewById(R.id.editPassword);
						password = passwordView.getText().toString();
						dialog.dismiss();
						receiver.applyCredentials(CredentialsDialog.this);
						
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				}).create();
		dialog.show();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
