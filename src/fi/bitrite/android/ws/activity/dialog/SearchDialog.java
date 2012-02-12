package fi.bitrite.android.ws.activity.dialog;

import roboguice.util.Strings;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import fi.bitrite.android.ws.activity.MainActivity;
import fi.bitrite.android.ws.auth.CredentialsProvider;
import fi.bitrite.android.ws.auth.CredentialsReceiver;
import fi.bitrite.android.ws.auth.CredentialsService;
import fi.bitrite.android.ws.auth.NoCredentialsException;

public class SearchDialog implements CredentialsReceiver {

	public static final int PROGRESS_DIALOG_TEXT_SEARCH = 1;

	int currentDialogId;
	
	ProgressDialog progressDialog;
	
	// TODO: inject these as well from constructor?
	CredentialsService credentialsService;
	MainActivity mainActivity;
	
	public SearchDialog(MainActivity parent, CredentialsService credentialsService) {
		this.mainActivity = parent;
		this.credentialsService = credentialsService;
	}
	
	public void showDialog(int id) {
		currentDialogId = id;
		
		try {
			credentialsService.applyStoredCredentials(this);
		}
		
		catch (NoCredentialsException e) {
			new CredentialsDialog(mainActivity, this).show();
		}
	}

	public void applyCredentials(CredentialsProvider provider) {
		String username = provider.getUsername();
		String password = provider.getPassword();
		
		if (Strings.isEmpty(username) || Strings.isEmpty(password)) {
			return;
		}
		
		credentialsService.storeCredentials(provider);
		
		mainActivity.showDialog(currentDialogId);	
		
		switch (currentDialogId) {
			case PROGRESS_DIALOG_TEXT_SEARCH:
				mainActivity.doTextSearch();
				break;
		}
	}
	
	public Dialog createDialog(int id) {
		progressDialog = new ProgressDialog(mainActivity);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage("Performing search ...");
		return progressDialog;
	}

	public void alertNoResults() {
		showAlertDialog("Your search yielded no results.");
	}

	private void showAlertDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setMessage(message).setCancelable(false)
				.setNeutralButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();		
	}
	
	public void alertError() {
		showAlertDialog("Search failed. Check your credentials and internet connection.");
	}
	
	public void dismiss() {
		mainActivity.dismissDialog(currentDialogId);
	}
}
