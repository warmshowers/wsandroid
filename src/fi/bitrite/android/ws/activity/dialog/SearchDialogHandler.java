package fi.bitrite.android.ws.activity.dialog;

import roboguice.util.Strings;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import fi.bitrite.android.ws.activity.MainActivity;
import fi.bitrite.android.ws.auth.CredentialsProvider;
import fi.bitrite.android.ws.auth.CredentialsReceiver;

public class SearchDialogHandler implements CredentialsReceiver {

	public static final int PROGRESS_DIALOG_TEXT_SEARCH = 1;

	ProgressDialog progressDialog;
	
	MainActivity mainActivity;
	
	public SearchDialogHandler(MainActivity parent) {
		this.mainActivity = parent;
	}

	public void showTextSearchDialog() {
		CredentialsDialog credentialsDialog = new CredentialsDialog(mainActivity, this);
		credentialsDialog.show();
	}

	public void applyCredentials(CredentialsProvider provider) {
		String username = provider.getUsername();
		String password = provider.getPassword();
		
		if (Strings.isEmpty(username) || Strings.isEmpty(password)) {
			return;
		}
		
		mainActivity.showDialog(SearchDialogHandler.PROGRESS_DIALOG_TEXT_SEARCH);	

		mainActivity.doTextSearch();
	}
	
	public Dialog createDialog(int id) {
		progressDialog = new ProgressDialog(mainActivity);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage("Performing search ...");
		return progressDialog;
	}

	public void alertNoResults() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setMessage("Your search yielded no results.").setCancelable(false)
				.setNeutralButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void dismiss() {
		mainActivity.dismissDialog(PROGRESS_DIALOG_TEXT_SEARCH);
	}
}
