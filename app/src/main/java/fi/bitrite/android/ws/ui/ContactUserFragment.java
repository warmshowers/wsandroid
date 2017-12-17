package fi.bitrite.android.ws.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.host.impl.RestHostContact;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.ui.util.DialogHelper;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.http.HttpException;

/**
 * Responsible for letting the user type in a message and then sending it to an other user
 * over the Warmshowers web service.
 */
public class ContactUserFragment extends BaseFragment {

    private final static String KEY_RECIPIENT = "recipient";

    @Inject AuthenticationController mAuthenticationController;

    @BindView(R.id.contact_user_txt_subject) EditText mTxtSubject;
    @BindView(R.id.contact_user_txt_message) EditText mTxtMessage;
    @BindView(R.id.all_btn_submit) Button mBtnSubmit;
    @BindView(R.id.all_lbl_no_network_warning) TextView mLblNoNetworkWarning;

    private Host mRecipient;

    private ProgressDialog.Disposable mProgressDisposable;
    private ContactUserTask mContactUserTask;

    public static Fragment create(Host recipient) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_RECIPIENT, recipient);

        Fragment fragment = new ContactUserFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_user, container, false);
        ButterKnife.bind(this, view);

        mRecipient = getArguments().getParcelable(KEY_RECIPIENT);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isConnected = Tools.isNetworkConnected(getContext());
        mLblNoNetworkWarning.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        mBtnSubmit.setEnabled(isConnected);
    }

    @OnClick(R.id.all_btn_submit)
    public void sendMessageToHost() {
        String subject = mTxtSubject.getText().toString();
        String message = String.format("%s\n\n%s",
                mTxtMessage.getText().toString(), getString(R.string.sent_from_android));

        if (TextUtils.isEmpty(subject) || TextUtils.isEmpty(message)) {
            DialogHelper.alert(getContext(), R.string.message_validation_error);
            return;
        }

        mProgressDisposable = ProgressDialog.create(R.string.sending_message)
                .show(getActivity());

        mContactUserTask = new ContactUserTask();
        mContactUserTask.execute(subject, message);
    }

    private class ContactUserTask extends AsyncTask<String, Void, Object> {

        @Override
        protected Object doInBackground(String... params) {
            String subject = params[0];
            String message = params[1];

            try {
                RestHostContact contact = new RestHostContact(mAuthenticationController);
                JSONObject result = contact.send(mRecipient.getName(), subject, message);

                JSONArray resultArray = result.getJSONArray("arrayresult");

                if (!resultArray.getBoolean(0)) {
                    throw new HttpException("Failed to send contact request, inappropriate result: " + resultArray);
                }

                return null;
            } catch (Exception e) {
                Log.e(WSAndroidApplication.TAG, e.getMessage(), e);
                return e;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            mProgressDisposable.dispose();

            if (result instanceof Exception) {
                RestClient.reportError(getContext(), result);
                return;
            }

            showSuccessDialog();
        }
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(getContext())
                .setMessage(getResources().getString(R.string.message_sent))
                .setPositiveButton(getResources().getString(R.string.ok),
                        (dialog, id) -> getActivity().getSupportFragmentManager().popBackStack())
                .create()
                .show();
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_contact_user, mRecipient.getFullname());
    }
}
