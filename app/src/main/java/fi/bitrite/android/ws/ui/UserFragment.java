package fi.bitrite.android.ws.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindColor;
import butterknife.BindInt;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.api.helper.HttpErrorHelper;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.FavoriteRepository;
import fi.bitrite.android.ws.repository.FeedbackRepository;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.listadapter.FeedbackListAdapter;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.WSGlide;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

import static android.graphics.PorterDuff.Mode.MULTIPLY;

/**
 * Activity that fetches user information and shows it to the user.
 * The information is retrieved either from the device storage (for starred users)
 * or downloaded from the WarmShowers web service.
 */
public class UserFragment extends BaseFragment {
    public static final String TAG = "UserFragment";

    private static final String KEY_USER_ID = "user_id";

    @Inject FavoriteRepository mFavoriteRepository;
    @Inject FeedbackRepository mFeedbackRepository;
    @Inject UserRepository mUserRepository;

    @BindView(R.id.user_swipe_refresh) SwipeRefreshLayout mSwipeRefresh;
    @BindView(R.id.user_layout_details) LinearLayout mLayoutDetails;
    @BindView(R.id.user_img_photo) ImageView mImgPhoto;
    @BindView(R.id.user_img_expanded) ImageView mImgExpanded;
    @BindView(R.id.user_lbl_name) TextView mLblName;

    @BindView(R.id.user_img_favorite) ImageView mImgFavorite;
    @BindView(R.id.user_ckb_favorite) CheckBox mCkbFavorite;

    @BindView(R.id.user_lbl_availability) TextView mLblAvailability;
    @BindView(R.id.user_img_availability) ImageView mImgAvailability;
    @BindView(R.id.user_lbl_login_info) TextView mLblLoginInfo;
    @BindView(R.id.user_lbl_limitations) TextView mLblLimitations;
    @BindView(R.id.user_lbl_location) TextView mLblLocation;
    @BindView(R.id.user_lbl_phone) TextView mLblPhone;
    @BindView(R.id.user_lbl_services) TextView mLblServices;
    @BindView(R.id.user_lbl_nearby_services) TextView mLblNearbyServices;
    @BindView(R.id.user_lbl_comments) TextView mLblComments;

    @BindView(R.id.user_lbl_feedback) TextView mLblFeedback;
    @BindView(R.id.user_lst_feedback) RecyclerView mLstFeedback;

    @BindColor(R.color.primaryColorAccent) int mFavoritedColor;
    @BindColor(R.color.primaryTextColor) int mNonFavoritedColor;

    @BindColor(R.color.rating_positive) int mRatingColorPositive;
    @BindColor(R.color.rating_neutral) int mRatingColorNeutral;
    @BindColor(R.color.rating_negative) int mRatingColorNegative;

    @BindInt(android.R.integer.config_shortAnimTime) int mShortAnimationDuration;

    private BehaviorSubject<UserInfoLoadResult> mLastUserInfoLoadResult = BehaviorSubject.create();

    private int mUserId;

    private final BehaviorSubject<User> mUser = BehaviorSubject.create();
    private final BehaviorSubject<List<Feedback>> mFeedbacks = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> mFavorite = BehaviorSubject.create();

    private boolean mIsImgExpandible;
    private Animator mCurrentAnimator;

    private boolean mDbFavoriteStatus;
    private FeedbackListAdapter mFeedbackListAdapter;

    public static Fragment create(int userId) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_USER_ID, userId);

        Fragment fragment = new UserFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);
        ButterKnife.bind(this, view);

        Bundle arguments = getArguments();

        mUserId = arguments.getInt(KEY_USER_ID);

        mSwipeRefresh.setOnRefreshListener(this::reloadUserInformation);

        mDbFavoriteStatus = mFavoriteRepository.isFavorite(mUserId);
        mFavorite.onNext(mDbFavoriteStatus);

        loadUserInformation();
        mLayoutDetails.setVisibility(View.GONE);

        mFeedbackListAdapter = new FeedbackListAdapter(mUserRepository);
        mFeedbackListAdapter.setOnUserClickHandler(
                user -> getNavigationController().navigateToUser(user.id));
        mLstFeedback.setAdapter(mFeedbackListAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        getResumePauseDisposable().add(mUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> updateUserViewContent()));

        getResumePauseDisposable().add(mFeedbacks
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(feedbacks -> {
                    mFeedbackListAdapter.replace(new ArrayList<>(feedbacks));
                    if (feedbacks.isEmpty()) {
                        mLblFeedback.setText(getString(R.string.no_feedback_yet));
                    } else {
                        int positiveCount = 0;
                        int neutralCount = 0;
                        int negativeCount = 0;
                        for (Feedback feedback : feedbacks) {
                            switch (feedback.rating) {
                                case Positive: ++positiveCount; break;
                                case Neutral: ++neutralCount; break;
                                case Negative: ++negativeCount; break;
                                default: throw new RuntimeException("Unknown rating");
                            }
                        }

                        RatingCountStringBuilder rcsb =
                                new RatingCountStringBuilder(getString(R.string.feedback));
                        rcsb.appendRatingCount(positiveCount, mRatingColorPositive);
                        rcsb.appendRatingCount(neutralCount, mRatingColorNeutral);
                        rcsb.appendRatingCount(negativeCount, mRatingColorNegative);
                        mLblFeedback.setText(rcsb.build());
                    }
                }));

        getResumePauseDisposable().add(mFavorite
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isFavorite -> {
                    saveUserIfFavorite();
                    mCkbFavorite.setChecked(isFavorite);
                    mImgFavorite.setColorFilter(isFavorite ? mFavoritedColor : mNonFavoritedColor);
                }));

        getResumePauseDisposable().add(mLastUserInfoLoadResult
                .observeOn(AndroidSchedulers.mainThread())
                .filter(result -> !result.isHandled)
                .doOnNext(result -> {
                    result.isHandled = true;

                    mSwipeRefresh.setRefreshing(false);

                    if (result.throwable != null) {
                        HttpErrorHelper.showErrorToast(getContext(), result.throwable);
                        if (mUser.getValue() == null) {
                            getFragmentManager().popBackStack();
                        }
                    }
                })
                .subscribe());
    }

    private void saveUserIfFavorite() {
        boolean isFavorite = mFavorite.getValue();
        if (isFavorite == mDbFavoriteStatus) {
            return;
        }

        User user = mUser.getValue();
        List<Feedback> feedback = mFeedbacks.getValue();

        if (isFavorite) {
            mFavoriteRepository.add(user, feedback);
        } else {
            mFavoriteRepository.remove(user.id);
        }

        mDbFavoriteStatus = isFavorite;
    }

    @OnCheckedChanged(R.id.user_ckb_favorite)
    void onFavoriteSelected(CompoundButton button, boolean isChecked) {
        if (mFavorite.getValue() == isChecked) {
            return;
        }

        mFavorite.onNext(isChecked);

        int msgId = isChecked
                ? R.string.user_starred
                : R.string.user_unstarred;
        Toast.makeText(getContext(), msgId, Toast.LENGTH_SHORT).show();
    }

    private void updateUserViewContent() {
        mLayoutDetails.setVisibility(View.VISIBLE);

        final User user = mUser.getValue();

        mLblName.setText(user.getName());
        setTitle(user.getName());

        // User Availability:
        // Set the user icon to black if they're available, otherwise gray
        boolean isAvailable = user.isCurrentlyAvailable;
        mImgAvailability.setAlpha(isAvailable ? 1.0f : 0.5f);
        mImgAvailability.setImageResource(R.drawable.ic_home_grey600_24dp);
        mImgAvailability.setColorFilter(isAvailable
                        ? Color.parseColor("#FF000000")
                        : Color.parseColor("#FF757575"),
                MULTIPLY);

        mLblAvailability.setText(isAvailable
                ? R.string.currently_available
                : R.string.not_currently_available);

        DateFormat simpleDate = DateFormat.getDateInstance();
        String activeDate = simpleDate.format(user.lastAccess);
        String createdDate =
                new SimpleDateFormat("yyyy", Tools.getLocale(getContext())).format(user.created);

        mLblLoginInfo.setText(getString(R.string.search_user_summary, createdDate, activeDate));
        mLblLimitations.setText(getString(
                R.string.user_limitations, user.maximalCyclistCount, user.spokenLanguages));


        // User location section
        mLblLocation.setText(user.getFullAddress());
        List<String> phones = new ArrayList<>();
        if (!TextUtils.isEmpty(user.mobilePhone)) {
            phones.add(getString(R.string.mobile_phone_abbrev, user.mobilePhone));
        }
        if (!TextUtils.isEmpty(user.homePhone)) {
            phones.add(getString(R.string.home_phone_abbrev, user.homePhone));
        }
        if (!TextUtils.isEmpty(user.workPhone)) {
            phones.add(getString(R.string.work_phone_abbrev, user.workPhone));
        }

        mLblPhone.setVisibility(phones.isEmpty() ? View.GONE : View.VISIBLE);
        if (!phones.isEmpty()) {
            mLblPhone.setText(TextUtils.join("\n", phones));
            Linkify.addLinks(mLblPhone, Linkify.ALL);
        }

        // Profile/Comments section
        // Convert text ("About me" == Comments from user data) to form to add to TextView
        mLblComments.setText(Html.fromHtml(user.comments.replace("\n", "<br>")));

        // User Services
        String userServices = user.getUserServices(getContext());
        mLblServices.setVisibility(userServices.isEmpty() ? View.GONE : View.VISIBLE);
        if (!userServices.isEmpty()) {
            mLblServices.setText(getString(R.string.user_services_description, userServices));
        }

        // Nearby Services
        String nearbyServices = user.getNearbyServices(getContext());
        mLblNearbyServices.setVisibility(nearbyServices.isEmpty() ? View.GONE : View.VISIBLE);
        if (!nearbyServices.isEmpty()) {
            mLblNearbyServices.setText(
                    getString(R.string.nearby_services_description, nearbyServices));
        }

        // If we're connected and there is a picture, get user picture.
        mIsImgExpandible = false;
        mImgPhoto.setImageResource(R.drawable.default_userinfo_profile);
        String url = user.profilePicture.getLargeUrl();
        if (!TextUtils.isEmpty(url)) {
            WSGlide.with(requireContext())
                    .load(url)
                    .addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onResourceReady(Drawable resource,
                                                       Object model,
                                                       Target<Drawable> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {
                            mIsImgExpandible = true;
                            mImgPhoto.setImageDrawable(resource);
                            mImgExpanded.setImageDrawable(resource);
                            return false;
                        }

                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e,
                                                    Object model,
                                                    Target<Drawable> target,
                                                    boolean isFirstResource) {
                            return false;
                        }
                    })
                    .preload();
            mImgPhoto.setContentDescription(
                    getString(R.string.content_description_avatar_of_var, user.getName()));
        }
    }

    private void contactUser(@NonNull User user) {
        getNavigationController().navigateToContactUser(user);
    }

    private void sendFeedback(@NonNull User user) {
        getNavigationController().navigateToFeedback(user.id);
    }

    private void showUserOnMap(@NonNull User user) {
        getNavigationController().navigateToMap(user.location);
    }

    /**
     * Send a geo intent so that we can view the user on external maps application
     */
    public void sendGeoIntent(@NonNull User user) {
        String lat = Double.toString(user.location.getLatitude());
        String lng = Double.toString(user.location.getLongitude());
        String query = Uri.encode(lat + "," + lng + "(" + user.getName()+ ")");
        Uri uri = Uri.parse("geo:" + lat + "," + lng + "?q=" + query);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void viewOnSite(@NonNull User user) {
        String url = getString(R.string.url_website_base) + "/user/" + user.id;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private void loadUserInformation() {
        // Structure for decoupling the user load callback, that is processed upon arrival, from
        // its handler that can only be executed when the app is in the foreground. Callback.
        mSwipeRefresh.setRefreshing(true);
        UserInfoLoadResult result = new UserInfoLoadResult();
        Disposable unused = Maybe.mergeDelayError(
                mUserRepository.get(mUserId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .filter(userResource -> {
                            if (userResource.hasData()) {
                                mUser.onNext(userResource.data);
                            }
                            if (userResource.isError()) {
                                result.throwable = userResource.error;
                            }
                            return !userResource.isLoading();
                        })
                        .firstElement(),
                mFeedbackRepository.getForRecipient(mUserId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .filter(feedbacksResource -> {
                            if (feedbacksResource.hasData()) {
                                mFeedbacks.onNext(feedbacksResource.data);
                            }
                            if (feedbacksResource.isError()) {
                                result.throwable = feedbacksResource.error;
                            }
                            return !feedbacksResource.isLoading();
                        })
                        .firstElement())
                .doOnComplete(() -> mLastUserInfoLoadResult.onNext(result))
                .doOnError(throwable -> {
                    result.throwable = throwable;
                    mLastUserInfoLoadResult.onNext(result);
                })
                .subscribe();
    }

    @OnClick(R.id.user_img_photo)
    void zoomImageFromThumb() {
        if (!mIsImgExpandible) {
            return;
        }

        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        mImgExpanded.setImageDrawable(mImgPhoto.getDrawable());

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        mImgPhoto.getGlobalVisibleRect(startBounds);
        mSwipeRefresh.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
            > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        mImgPhoto.setAlpha(0.3f);
        mImgExpanded.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        mImgExpanded.setPivotX(0f);
        mImgExpanded.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(mImgExpanded, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(mImgExpanded, View.Y, startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(mImgExpanded, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(mImgExpanded, View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        mImgExpanded.setOnClickListener(view -> {
            if (mCurrentAnimator != null) {
                mCurrentAnimator.cancel();
            }

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            AnimatorSet set1 = new AnimatorSet();
            set1
                    .play(ObjectAnimator.ofFloat(mImgExpanded, View.X, startBounds.left))
                    .with(ObjectAnimator.ofFloat(mImgExpanded, View.Y,startBounds.top))
                    .with(ObjectAnimator.ofFloat(mImgExpanded, View.SCALE_X, startScaleFinal))
                    .with(ObjectAnimator.ofFloat(mImgExpanded, View.SCALE_Y, startScaleFinal));
            set1.setDuration(mShortAnimationDuration);
            set1.setInterpolator(new DecelerateInterpolator());
            set1.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mImgPhoto.setAlpha(1f);
                    mImgExpanded.setVisibility(View.GONE);
                    mCurrentAnimator = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mImgPhoto.setAlpha(1f);
                    mImgExpanded.setVisibility(View.GONE);
                    mCurrentAnimator = null;
                }
            });
            set1.start();
            mCurrentAnimator = set1;
        });
    }

    private void reloadUserInformation() {
        mUserRepository.markAsOld(mUserId);
        mFeedbackRepository.markAsOldForRecipient(mUserId);
        loadUserInformation();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.user_information_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        User user = mUser.getValue();

        switch (item.getItemId()) {
            case R.id.menuSendMessage:
                contactUser(user);
                return true;
            case R.id.menuViewOnMap:
                showUserOnMap(user);
                return true;
            case R.id.menuMapApplication:
                sendGeoIntent(user);
                return true;
            case R.id.menuLeaveFeedback:
                sendFeedback(user);
                return true;
            case R.id.menuViewOnSite:
                viewOnSite(user);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_user);
    }

    private class UserInfoLoadResult {
        Throwable throwable = null;
        boolean isHandled = false;
    }

    /**
     * Helps creating the rating count string. It consists of the non-zero rating counts with their
     * respective color.
     */
    private static class RatingCountStringBuilder {
        private final SpannableStringBuilder mRatingString;
        private boolean mPrependSeperator = false;

        RatingCountStringBuilder(String initialString) {
            mRatingString = new SpannableStringBuilder(initialString + " (");
        }

        void appendRatingCount(int count, @ColorInt int color) {
            if (count == 0) {
                return;
            }

            int start = mRatingString.length();
            if (mPrependSeperator) {
                mRatingString.append('/');
                ++start;
            }
            mPrependSeperator = true;

            mRatingString.append(Integer.toString(count));
            mRatingString.setSpan(new ForegroundColorSpan(color), start, mRatingString.length(), 0);
        }

        SpannableStringBuilder build() {
            mRatingString.append(')');
            return mRatingString;
        }
    }
}

