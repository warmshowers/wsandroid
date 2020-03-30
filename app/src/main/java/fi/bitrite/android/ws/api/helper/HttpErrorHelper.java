package fi.bitrite.android.ws.api.helper;


import android.content.Context;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;

import androidx.annotation.StringRes;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.util.Tools;
import retrofit2.HttpException;

public class HttpErrorHelper {

    @StringRes
    public static int getErrorStringRes(Throwable throwable) {
        if (throwable instanceof HttpException) {
            HttpException httpError = (HttpException) throwable;
            if (httpError.code() == 403) {
                return R.string.access_denied;
            } else if (httpError.code() >= 500) {
                return R.string.internal_server_error;
            } else {
                return R.string.http_server_access_failure;
            }
        } else if (throwable instanceof IOException) {
            return R.string.io_error;
        } else if (throwable instanceof JSONException) {
            return R.string.json_error;
        } else {
            return R.string.http_unexpected_failure;
        }
    }

    public static void showErrorToast(Context context, Throwable throwable) {
        int rId = HttpErrorHelper.getErrorStringRes(throwable);
        Toast.makeText(context, rId, Toast.LENGTH_LONG)
                .show();

        String exceptionDescription = throwable.toString();
        if (throwable.getMessage() != null) {
            exceptionDescription += " Message:" + throwable.getMessage();
        }
        if (throwable.getCause() != null) {
            exceptionDescription += " Cause: " + throwable.getCause().toString();
        }
        Tools.gaReportException(context, "RestClient Exception: ", exceptionDescription);
    }
}
