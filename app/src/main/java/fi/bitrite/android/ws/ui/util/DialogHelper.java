package fi.bitrite.android.ws.ui.util;

import android.content.Context;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

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
