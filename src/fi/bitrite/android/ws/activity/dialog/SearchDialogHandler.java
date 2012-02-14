package fi.bitrite.android.ws.activity.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import fi.bitrite.android.ws.activity.MainActivity;

public class SearchDialogHandler {

	public static final int NO_SEARCH 	= 0;
	public static final int TEXT_SEARCH = 1;

	int currentDialogId;
	
	ProgressDialog progressDialog;
	
	MainActivity mainActivity;
	
	public SearchDialogHandler(MainActivity parent) {
		this.mainActivity = parent;
	}

	public void prepareSearch(int searchId) {
		currentDialogId = searchId;
	}
	
	public void doSearch() {
		if (currentDialogId == NO_SEARCH) {
			throw new NoPreparedSearchException();
		}
		
		mainActivity.showDialog(currentDialogId);	
		
		switch (currentDialogId) {
			case TEXT_SEARCH:
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
		currentDialogId = NO_SEARCH;
	}

	public boolean isSearchInProgress() {
		return currentDialogId != NO_SEARCH;
	}

}
