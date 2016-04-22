package fi.bitrite.android.ws.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;

public class ProgressDialogFragment extends DialogFragment {

    public static final String DIALOG_MESSAGE = "message";

    @StringRes
    private int message;

    public static ProgressDialogFragment newInstance(@StringRes int message) {
        ProgressDialogFragment fragment = new ProgressDialogFragment();

        final Bundle args = new Bundle();
        args.putInt(DIALOG_MESSAGE, message);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getArguments().isEmpty()) {
            final Bundle args = getArguments();
            message = args.getInt(DIALOG_MESSAGE);
        }

        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());

        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(message));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        return progressDialog;
    }
}
