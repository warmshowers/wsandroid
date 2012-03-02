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

	public static final int TEXT_SEARCH = 1;

	public static final int AUTHENTICATE = 2;

	int currentDialogId;
	
	ProgressDialog progressDialog;
	
	Activity parentActivity;
	
	public DialogHandler(Activity parent) {
		this.parentActivity = parent;
	}

	public void showDialog(int id) {
		parentActivity.showDialog(id);	
		currentDialogId = id;
	}

	public Dialog createDialog(int id, String message) {
		progressDialog = new ProgressDialog(parentActivity);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage(message);
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
	
	public void alertError(String message) {
		showAlertDialog(message);
	}
	
	public void dismiss() {
		parentActivity.dismissDialog(currentDialogId);
	}

}
