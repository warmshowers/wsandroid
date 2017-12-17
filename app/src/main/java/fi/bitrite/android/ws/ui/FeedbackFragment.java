package fi.bitrite.android.ws.ui;

import android.app.DatePickerDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.ui.util.DialogHelper;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.util.ArrayTranslator;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.Tools;

/**
 * Responsible for letting the user type in a message and then sending it to a host
 * over the WarmShowers web service.
 */
public class FeedbackFragment extends BaseFragment {

    private final static String KEY_RECIPIENT = "recipient";

    // This value must match the "minimum number of words" in the node submission settings at
    // https://www.warmshowers.org/admin/content/node-type/trust-referral
    private final static int MIN_FEEDBACK_WORD_LENGTH = 10;

    @Inject AuthenticationController mAuthenticationController;

    @BindView(R.id.feedback_txt_feedback) EditText mTxtFeedback;
    @BindView(R.id.feedback_txt_date_we_met) EditText mTxtDateWeMet;
    @BindView(R.id.feedback_lbl_overall_experience) TextView mLblOverallExperience;
    @BindView(R.id.feedback_sel_overall_experience) Spinner mSelOverallExperience;
    @BindView(R.id.feedback_sel_how_we_met) Spinner mSelHowWeMet;
    @BindView(R.id.all_btn_submit) Button mBtnSubmit;
    @BindView(R.id.all_lbl_no_network_warning) TextView mLblNoNetworkWarning;

    private int mDateWeMetMonth;
    private int mDateWeMetYear;

    private DatePickerDialog mDatePickerDialog;
    private final ArrayTranslator mTranslator = ArrayTranslator.getInstance();
    private ProgressDialog.Disposable mProgressDisposable;

    private Host mRecipient;

    public static Fragment create(Host recipient) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_RECIPIENT, recipient);

        Fragment fragment = new FeedbackFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);
        ButterKnife.bind(this, view);

        // Set mTxtDateWeMet to current date for default
        Calendar now = Calendar.getInstance();
        String hostedOn = Tools.getDateAsMY(getContext(), now.getTimeInMillis());
        mTxtDateWeMet.setText(hostedOn);
        mDateWeMetMonth = now.get(Calendar.MONTH);
        mDateWeMetYear = now.get(Calendar.YEAR);

        mRecipient = getArguments().getParcelable(KEY_RECIPIENT);

        mDatePickerDialog = new DatePickerDialog(getContext(), (v, year, monthOfYear, dayOfMonth) -> {
            mDateWeMetMonth = monthOfYear;
            mDateWeMetYear = year;

            Calendar date = Calendar.getInstance();
            date.set(year, monthOfYear, dayOfMonth);
            mTxtDateWeMet.setText(Tools.getDateAsMY(getContext(), date.getTimeInMillis()));
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

        mLblOverallExperience.setText(getString(R.string.lbl_feedback_overall_experience, mRecipient.getFullname()));

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
    public void sendFeedback() {
        // Site requires 10 words in the feedback, so pre-enforce that.
        if (mTxtFeedback.getText().toString().split("\\w+").length < MIN_FEEDBACK_WORD_LENGTH) {
            DialogHelper.alert(getContext(), R.string.feedback_validation_error);
            return;
        }
        // Ensure a selection in the "how we met"
        if (mSelHowWeMet.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
            DialogHelper.alert(getContext(), R.string.feedback_how_we_met_error);
            return;
        }
        // Ensure a selection in "overall experience"
        if (mSelOverallExperience.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
            DialogHelper.alert(getContext(), R.string.feedback_overall_experience_error);
            return;
        }

        mProgressDisposable = ProgressDialog.create(R.string.sending_feedback)
                .show(getActivity());
        SendFeedbackTask task = new SendFeedbackTask();
        task.execute();
    }

    @OnClick(R.id.feedback_txt_date_we_met)
    public void onDateWeMetClick() {
        mDatePickerDialog.show();
    }

    private class SendFeedbackTask extends AsyncTask<Void, Void, Object> {
        private final static String FEEDBACK_POST_URL =
                GlobalInfo.warmshowersBaseUrl + "/services/rest/node";

        @Override
        protected Object doInBackground(Void[] params) {
            try {

                // See https://github.com/warmshowers/Warmshowers.org/wiki/Warmshowers-RESTful-Services-for-Mobile-Apps#create_feedback
                List<NameValuePair> args = new ArrayList<NameValuePair>();

                // Drupal 7 semantics for node creation
//                args.add(new BasicNameValuePair("type", "trust_referral"));
//                args.add(new BasicNameValuePair("field_member_i_trust[und][0][uid]", host.getName()));
//                args.add(new BasicNameValuePair("body[und][0][value]", mTxtFeedback.getText().toString()));
//                args.add(new BasicNameValuePair("field_guest_or_host[und]", mTranslator.getEnglishHostGuestOption(mSelHowWeMet.getSelectedItemPosition())));
//                args.add(new BasicNameValuePair("field_rating[und]", mTranslator.getEnglishRating(mSelOverallExperience.getSelectedItemPosition())));
//                args.add(new BasicNameValuePair("field_hosting_date[und][0][value][year]", Integer.toString(mDateWeMetYear)));
//                args.add(new BasicNameValuePair("field_hosting_date[und][0][value][month]", Integer.toString(mDateWeMetMonth + 1)));
//                args.add(new BasicNameValuePair("field_hosting_date[und][0][value][day]", "15")); // D7 required day of month

                // Drupal 6 semantics for node creation, wrapped on server side
                args.add(new BasicNameValuePair("node[type]", "trust_referral"));
                args.add(new BasicNameValuePair("node[field_member_i_trust][0][uid][uid]", mRecipient.getName()));
                args.add(new BasicNameValuePair("node[body]", mTxtFeedback.getText().toString()));
                args.add(new BasicNameValuePair("node[field_guest_or_host][value]", mTranslator.getEnglishHostGuestOption(mSelHowWeMet.getSelectedItemPosition())));
                args.add(new BasicNameValuePair("node[field_rating][value]", mTranslator.getEnglishRating(mSelOverallExperience.getSelectedItemPosition())));
                args.add(new BasicNameValuePair("node[field_hosting_date][0][value][year]", Integer.toString(mDateWeMetYear)));
                args.add(new BasicNameValuePair("node[field_hosting_date][0][value][month]", Integer.toString(mDateWeMetMonth + 1)));

                RestClient restClient = new RestClient(mAuthenticationController);
                JSONObject result = restClient.post(FEEDBACK_POST_URL, args);

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
                .setMessage(getResources().getString(R.string.feedback_sent, mRecipient.getFullname()))
                .setPositiveButton(R.string.ok,
                        (dialog, id) -> getActivity().getSupportFragmentManager().popBackStack())
                .create()
                .show();
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_feedback);
    }
}
