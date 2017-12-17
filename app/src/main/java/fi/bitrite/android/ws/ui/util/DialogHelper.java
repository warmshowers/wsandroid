package fi.bitrite.android.ws.ui.util;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;

import fi.bitrite.android.ws.R;

public class DialogHelper {

    public static void alert(Context context, @StringRes int messageId) {
        new AlertDialog.Builder(context)
                .setMessage(messageId)
                .setPositiveButton(R.string.alert_neutral_button, (dialog, id) -> dialog.dismiss())
                .setCancelable(true)
                .create()
                .show();
    }
}
