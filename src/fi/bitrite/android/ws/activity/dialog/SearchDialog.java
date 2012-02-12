package fi.bitrite.android.ws.activity.dialog;

import android.app.Dialog;

public interface SearchDialog {

	public void showTextSearchDialog();

	public Dialog createDialog(int id);

	public void alertNoResults();
	
	public void alertError();

	public void dismiss();

}