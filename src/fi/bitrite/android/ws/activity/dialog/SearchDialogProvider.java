package fi.bitrite.android.ws.activity.dialog;

import roboguice.util.Strings;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;

import com.google.inject.Inject;

import fi.bitrite.android.ws.activity.MainActivity;
import fi.bitrite.android.ws.auth.CredentialsProvider;
import fi.bitrite.android.ws.auth.CredentialsReceiver;
import fi.bitrite.android.ws.auth.CredentialsService;
import fi.bitrite.android.ws.auth.NoCredentialsException;

public class SearchDialogProvider implements CredentialsReceiver, SearchDialog {

	public static final int PROGRESS_DIALOG_TEXT_SEARCH = 1;

	ProgressDialog progressDialog;
	
	// TODO: inject these as well
	@Inject	CredentialsService credentialsService;
	
	@Inject	MainActivity mainActivity;
	
	@Inject
	public SearchDialogProvider(MainActivity parent, CredentialsService credentialsService) {
		this.mainActivity = parent;
		this.credentialsService = credentialsService;
	}
	
	public void showTextSearchDialog() {
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
		
		mainActivity.showDialog(SearchDialogProvider.PROGRESS_DIALOG_TEXT_SEARCH);	

		mainActivity.doTextSearch();
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
		mainActivity.dismissDialog(PROGRESS_DIALOG_TEXT_SEARCH);
	}
}
