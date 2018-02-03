package fi.bitrite.android.ws.ui;

import android.app.DatePickerDialog;
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

import java.util.Calendar;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api_new.WarmshowersService;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.repository.FeedbackRepository;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.util.DialogHelper;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.util.Tools;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Responsible for letting the user type in a message and then sending it to a host
 * over the WarmShowers web service.
 */
public class FeedbackFragment extends BaseFragment {

    private final static String KEY_RECIPIENT_ID = "recipient_id";

    @Inject FeedbackRepository mFeedbackRepository;
    @Inject UserRepository mUserRepository;

    @BindView(R.id.feedback_txt_feedback) EditText mTxtFeedback;
    @BindView(R.id.feedback_txt_date_we_met) EditText mTxtDateWeMet;
    @BindView(R.id.feedback_lbl_rating) TextView mLblRating;
    @BindView(R.id.feedback_sel_rating) Spinner mSelRating;
    @BindView(R.id.feedback_sel_relation) Spinner mSelRelation;
    @BindView(R.id.all_btn_submit) Button mBtnSubmit;
    @BindView(R.id.all_lbl_no_network_warning) TextView mLblNoNetworkWarning;

    private int mDateWeMetMonth;
    private int mDateWeMetYear;

    private DatePickerDialog mDatePickerDialog;
    private ProgressDialog.Disposable mProgressDisposable;

    private int mRecipientId;
    private String mRecipientFullname;

    public static Fragment create(int recipientId) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_RECIPIENT_ID, recipientId);

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

        mRecipientId = getArguments().getInt(KEY_RECIPIENT_ID);

        mDatePickerDialog = new DatePickerDialog(getContext(), (v, year, monthOfYear, dayOfMonth) -> {
            mDateWeMetMonth = monthOfYear;
            mDateWeMetYear = year;

            Calendar date = Calendar.getInstance();
            date.set(year, monthOfYear, dayOfMonth);
            mTxtDateWeMet.setText(Tools.getDateAsMY(getContext(), date.getTimeInMillis()));
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

        mUserRepository.get(mRecipientId)
                .filter(Resource::hasData)
                .map(userResource -> userResource.data)
                .firstOrError()
                .subscribe(recipient -> {
                    mRecipientFullname = recipient.getFullname();
                    mLblRating.setText(getString(
                            R.string.lbl_feedback_overall_experience, mRecipientFullname));
                });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isConnected = Tools.isNetworkConnected(getContext());
        mLblNoNetworkWarning.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        mBtnSubmit.setEnabled(isConnected);
    }

    @OnClick(R.id.feedback_txt_date_we_met)
    public void onDateWeMetClick() {
        mDatePickerDialog.show();
    }

    @OnClick(R.id.all_btn_submit)
    public void sendFeedback() {
        // Site requires 10 words in the feedback, so pre-enforce that.
        final String body = mTxtFeedback.getText().toString();
        if (body.split("\\w+").length < WarmshowersService.FEEDBACK_MIN_WORD_LENGTH) {
            DialogHelper.alert(getContext(), R.string.feedback_validation_error);
            return;
        }
        // Ensure a selection in the "how we met"
        if (mSelRelation.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
            DialogHelper.alert(getContext(), R.string.feedback_how_we_met_error);
            return;
        }
        // Ensure a selection in "overall experience"
        if (mSelRating.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
            DialogHelper.alert(getContext(), R.string.feedback_overall_experience_error);
            return;
        }

        mProgressDisposable = ProgressDialog.create(R.string.sending_feedback)
                .show(getActivity());

        Feedback.Relation relation = getSelectedRelation();
        Feedback.Rating rating = getSelectedRating();

        int dateWeMetMonth = mDateWeMetMonth + 1; // This is 0-based.
        mFeedbackRepository.giveFeedback(mRecipientId, body, relation, rating, mDateWeMetYear,
                                         dateWeMetMonth)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent(t -> mProgressDisposable.dispose())
                .subscribe(() -> getActivity().onBackPressed(), throwable -> {
                    Log.e(WSAndroidApplication.TAG, "Failed to send feedback", throwable);
                    new AlertDialog.Builder(getContext())
                            .setMessage(R.string.feedback_error_sending)
                            .create()
                            .show();
                });
    }

    private Feedback.Relation getSelectedRelation() {
        // Keep in sync with R.array.feedback_relation_options.
        switch (mSelRelation.getSelectedItemPosition()) {
            case 0: return Feedback.Relation.Guest;
            case 1: return Feedback.Relation.Host;
            case 2: return Feedback.Relation.MetWhileTraveling; // FIXME(saemy): Serialized to "MetWhileTraveling"?
            case 3: return Feedback.Relation.Other;
            default: throw new RuntimeException("Invalid option.");
        }
    }

    private Feedback.Rating getSelectedRating() {
        // Keep in sync with R.array.feedback_rating_options.
        Feedback.Rating rating;
        switch (mSelRating.getSelectedItemPosition()) {
            case 0: return Feedback.Rating.Positive;
            case 1: return Feedback.Rating.Neutral;
            case 2: return Feedback.Rating.Negative;
            default: throw new RuntimeException("Invalid option.");
        }
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_feedback);
    }
}
