package fi.bitrite.android.ws.ui.listadapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.UserRepository;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class FeedbackListAdapter extends
        DataBoundListAdapter<Feedback, FeedbackListAdapter.ItemBinding> {

    public final static Comparator<Feedback> DEFAULT_COMPARATOR =
            (left, right) -> right.meetingDate.compareTo(left.meetingDate);

    private final UserRepository mUserRepository;
    private final Comparator<Feedback> mComparator;

    public interface OnUserClickHandler {
        void handle(@NonNull User user);
    }
    private OnUserClickHandler mOnUserClickHandler;

    public FeedbackListAdapter(UserRepository userRepository) {
        this(userRepository, DEFAULT_COMPARATOR);
    }
    public FeedbackListAdapter(UserRepository userRepository, Comparator<Feedback> comparator) {
        mComparator = comparator;
        mUserRepository = userRepository;
    }

    public void setOnUserClickHandler(OnUserClickHandler handler) {
        mOnUserClickHandler = handler;
    }

    @Override
    protected ItemBinding createBinding(ViewGroup parent) {
        return new ItemBinding(parent);
    }

    @Override
    protected void sort(List<Feedback> feedbacks) {
        Collections.sort(feedbacks, mComparator);
    }

    @Override
    protected boolean areItemsTheSame(Feedback left, Feedback right) {
        return left.id == right.id;
    }

    @Override
    protected boolean areContentsTheSame(Feedback left, Feedback right) {
        return left.id == right.id
               && left.recipientId == right.recipientId
               && left.senderId == right.senderId
               && left.relation == right.relation
               && left.meetingDate.equals(right.meetingDate)
               && left.body.equals(right.body);
    }

    class ItemBinding implements DataBoundListAdapter.ViewDataBinding<Feedback> {

        @BindView(R.id.feedback_lbl_sender) TextView mLblSender;
        @BindView(R.id.feedback_lbl_body) TextView mLblBody;
        @BindView(R.id.feedback_img_relation_and_rating) ImageView mImgRelationAndRating;
        @BindView(R.id.feedback_lbl_meetingDate) TextView mLblMeetingDate;

        @BindDrawable(R.drawable.ic_bicycle_white_24dp) Drawable mRelationDrawableGuest;
        @BindDrawable(R.drawable.ic_home_grey600_24dp) Drawable mRelationDrawableHost;
        @BindDrawable(R.drawable.ic_people_grey600_24dp) Drawable mRelationDrawableMetWhileTraveling;
        @BindDrawable(R.drawable.ic_rounded_square_12dp) Drawable mRelationDrawableOther;
        @BindString(R.string.feedback_relation_guest) String mRelationStrGuest;
        @BindString(R.string.feedback_relation_host) String mRelationStrHost;
        @BindString(R.string.feedback_relation_met_while_traveling) String mRelationStrMetWhileTraveling;
        @BindString(R.string.feedback_relation_other) String mRelationStrOther;
        @BindString(R.string.feedback_relation_long_guest) String mRelationStrLongGuest;
        @BindString(R.string.feedback_relation_long_host) String mRelationStrLongHost;
        @BindString(R.string.feedback_relation_long_met_while_traveling) String mRelationStrLongMetWhileTraveling;
        @BindString(R.string.feedback_relation_long_other) String mRelationStrLongOther;

        @ColorInt private final int mRatingColorPositive = Color.rgb(0, 153, 0);
        @ColorInt private final int mRatingColorNeutral = Color.rgb(115, 115, 115);
        @ColorInt private final int mRatingColorNegative = Color.rgb(204, 0, 0);
        @BindString(R.string.feedback_rating_positive) String mRatingStrPositive;
        @BindString(R.string.feedback_rating_neutral) String mRatingStrNeutral;
        @BindString(R.string.feedback_rating_negative) String mRatingStrNegative;

        private final SimpleDateFormat mMeetingDateFormat = new SimpleDateFormat("MMM ''yy", Locale.US);

        private final View mRoot;
        private CompositeDisposable mDisposables = new CompositeDisposable();

        private String mSenderName;
        private String mRecipientName;

        ItemBinding(ViewGroup parent) {
            mRoot = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_feedback, parent, false);
            ButterKnife.bind(this, mRoot);
        }

        @Override
        public View getRoot() {
            return mRoot;
        }

        @Override
        public void bind(@NonNull Feedback feedback) {
            mDisposables.dispose();
            mDisposables = new CompositeDisposable();

            // Sets the sender.
            mSenderName = null;
            mRecipientName = null;
            mLblSender.setOnClickListener(null);
            mDisposables.add(mUserRepository.get(feedback.recipientId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(userResource -> {
                        final User recipient = userResource.data;
                        mRecipientName = recipient == null ? "" : recipient.getName();
                        setRelationAndRating(feedback);
                    }));

            mDisposables.add(mUserRepository.get(feedback.senderId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(userResource -> {
                        final User sender = userResource.data;
                        mSenderName = sender == null ? "" : sender.getName();
                        mLblSender.setText(mSenderName);

                        mLblSender.setOnClickListener((view) -> {
                            if (mOnUserClickHandler != null && sender != null) {
                                mOnUserClickHandler.handle(sender);
                            }
                        });

                        setRelationAndRating(feedback);
                    }));

            mLblMeetingDate.setText(mMeetingDateFormat.format(feedback.meetingDate));
            mLblBody.setText(Html.fromHtml(feedback.body));

            setRelationAndRating(feedback);
        }

        private void setRelationAndRating(@NonNull Feedback feedback) {
            // Relation
            Drawable relationDrawable;
            int padding = 0;
            String relationStrShort;
            String relationStrLong;
            switch (feedback.relation) {
                // The feedback keeps the role of the recipient of the feedback.
                // We depict the role of the sender.

                case Guest:
                    relationDrawable = mRelationDrawableHost;
                    relationStrShort = mRelationStrHost;
                    relationStrLong = mRelationStrLongHost;
                    break;

                case Host:
                    relationDrawable = mRelationDrawableGuest;
                    relationStrShort = mRelationStrGuest;
                    relationStrLong = mRelationStrLongGuest;
                    break;

                case MetWhileTraveling:
                    relationDrawable = mRelationDrawableMetWhileTraveling;
                    relationStrShort = mRelationStrMetWhileTraveling;
                    relationStrLong = mRelationStrLongMetWhileTraveling;
                    break;

                case Other:
                    relationDrawable = mRelationDrawableOther;
                    relationStrShort = mRelationStrOther;
                    relationStrLong = mRelationStrLongOther;
                    padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                            mImgRelationAndRating.getResources().getDisplayMetrics());
                    break;

                default:
                    throw new RuntimeException("Unknown relation");
            }

            // Rating
            int ratingColor;
            String ratingStr;
            switch (feedback.rating) {
                case Positive:
                    ratingColor = mRatingColorPositive;
                    ratingStr = mRatingStrPositive;
                    break;

                case Neutral:
                    ratingColor = mRatingColorNeutral;
                    ratingStr = mRatingStrNeutral;
                    break;

                case Negative:
                    ratingColor = mRatingColorNegative;
                    ratingStr = mRatingStrNegative;
                    break;

                default:
                    throw new RuntimeException("Unknown rating");
            }
            mImgRelationAndRating.setImageDrawable(relationDrawable);
            mImgRelationAndRating.setColorFilter(ratingColor);
            mImgRelationAndRating.setPadding(padding, padding, padding, padding);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String relationStr = TextUtils.isEmpty(mRecipientName) || TextUtils.isEmpty(mSenderName)
                        ? relationStrShort
                        : String.format(relationStrLong, mSenderName, mRecipientName);
                mImgRelationAndRating.setTooltipText(relationStr + ", " + ratingStr);
            }
        }
    }
}
