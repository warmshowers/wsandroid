package fi.bitrite.android.ws.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;

/** 
 * Helper class for dialogs
 *
 */
public class DialogHandler {

	public static final int NO_DIALOG = 0;
	
	public static final int TEXT_SEARCH = 1;

	public static final int AUTHENTICATE = 2;

	public static final int HOST_INFORMATION = 3;

	public static final int HOST_CONTACT = 4;
	
	private static boolean inProgress = false;
	private static int  currentDialogId = NO_DIALOG;
	
	ProgressDialog progressDialog;
	
	Activity parentActivity;
	
	public DialogHandler(Activity parent) {
		this.parentActivity = parent;
	}

	public void showDialog(int id) {
		if (currentDialogId != id) {
			currentDialogId = id;
			inProgress = true;
			parentActivity.showDialog(id);
		}
	}

	public Dialog createDialog(int id, String message) {
		progressDialog = new ProgressDialog(parentActivity);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage(message);
		progressDialog.setCancelable(false);
		return progressDialog;
	}

	private void showAlertDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
		builder.setMessage(message).setCancelable(false)
				.setNeutralButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();		
	}
	
	public void alert(String message) {
		showAlertDialog(message);
	}
	
	public void dismiss() {
		inProgress = false;

		try {
			parentActivity.dismissDialog(currentDialogId);
		} 
		
		catch (IllegalArgumentException e) {
			// OK - we assume it has been closed earlier
		}
		
		currentDialogId = NO_DIALOG;		
	}
	
	public static boolean inProgress() {
		return inProgress;
	}
}
