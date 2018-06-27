package fi.bitrite.android.ws.ui.util;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.disposables.Disposable;

public class ProgressDialog extends DialogFragment {

    private static final String DIALOG_MESSAGE = "message";

    private final static AtomicInteger mDialogId = new AtomicInteger(0);

    @StringRes private int mMessage;

    public static ProgressDialog create(@StringRes int message) {
        final Bundle args = new Bundle();
        args.putInt(DIALOG_MESSAGE, message);

        ProgressDialog fragment = new ProgressDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args != null && !args.isEmpty()) {
            mMessage = args.getInt(DIALOG_MESSAGE);
        }

        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final android.app.ProgressDialog dialog =
                new android.app.ProgressDialog(getActivity());

        dialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(getString(mMessage));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    public Disposable show(@NonNull FragmentActivity parent) {
        String tag = "progressDialog-" + Integer.toString(mDialogId.getAndIncrement());
        FragmentManager fragmentManager = parent.getSupportFragmentManager();

        show(fragmentManager, tag);

        return new Disposable() {
            private boolean mDisposed;

            @Override
            public void dispose() {
                dismiss(); // Idempotent
                mDisposed = true;
            }
            @Override
            public boolean isDisposed() {
                return mDisposed;
            }
        };
    }
}
