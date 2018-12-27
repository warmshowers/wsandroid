package fi.bitrite.android.ws.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.api.helper.HttpErrorHelper;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.FavoriteRepository;
import fi.bitrite.android.ws.repository.FeedbackRepository;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.ui.view.FeedbackTable;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.ObjectUtils;
import fi.bitrite.android.ws.util.Tools;
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

    @BindView(R.id.user_layout_details) LinearLayout mLayoutDetails;
    @BindView(R.id.user_img_photo) ImageView mImgPhoto;
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
    @BindView(R.id.user_tbl_feedback) FeedbackTable mTblFeedback;

    @BindColor(R.color.primaryColorAccent) int mFavoritedColor;
    @BindColor(R.color.primaryTextColor) int mNonFavoritedColor;

    private BehaviorSubject<UserInfoLoadResult> mLastUserInfoLoadResult = BehaviorSubject.create();
    private Disposable mDownloadUserInfoProgressDisposable;

    private int mUserId;

    private final BehaviorSubject<User> mUser = BehaviorSubject.create();
    private final BehaviorSubject<List<Feedback>> mFeedbacks = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> mFavorite = BehaviorSubject.create();

    private boolean mDbFavoriteStatus;

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

        mDbFavoriteStatus = mFavoriteRepository.isFavorite(mUserId);
        mFavorite.onNext(mDbFavoriteStatus);

        if (!mLastUserInfoLoadResult.hasValue()) {
            loadUserInformation();
        }
        mLayoutDetails.setVisibility(View.GONE);
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
                .subscribe(feedbacks -> updateFeedbacksViewContent()));

        getResumePauseDisposable().add(mFavorite
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isFavorite -> {
                    saveUserIfFavorite();
                    mCkbFavorite.setChecked(isFavorite);
                    mImgFavorite.setColorFilter(isFavorite ? mFavoritedColor : mNonFavoritedColor);
                }));

        getResumePauseDisposable().add(mLastUserInfoLoadResult
                .filter(result -> !result.isHandled)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    result.isHandled = true;

                    mDownloadUserInfoProgressDisposable.dispose();
                    if (result.throwable != null) {
                        HttpErrorHelper.showErrorToast(getContext(), result.throwable);
                        if (mUser.getValue() == null) {
                            getFragmentManager().popBackStack();
                        }
                    }
                }));
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
        // Allow such TextView html as it will; but Drupal's text assumes linefeeds break lines
        mLblComments.setText(Tools.siteHtmlToHtml(user.comments));

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
        String url = user.profilePicture.getLargeUrl();
        if (!TextUtils.isEmpty(url)) {
            Picasso.with(getContext())
                    .load(url)
                    .placeholder(R.drawable.default_userinfo_profile)
                    .into(mImgPhoto);
            mImgPhoto.setContentDescription(
                    getString(R.string.content_description_avatar_of_var, user.getName()));
        }
    }

    private void updateFeedbacksViewContent() {
        List<Feedback> feedbacks = mFeedbacks.getValue();
        Collections.sort(feedbacks,
                (left, right) -> ObjectUtils.compare(right.meetingDate, left.meetingDate));

        mTblFeedback.setRows(feedbacks);
        mLblFeedback.setText(feedbacks.isEmpty()
                ? getString(R.string.no_feedback_yet)
                : getString(R.string.feedback, feedbacks.size()));
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

    public void viewOnSite(@NonNull User user) {
        String url = GlobalInfo.warmshowersBaseUrl + "/user/" + user.id;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private void loadUserInformation() {
        mDownloadUserInfoProgressDisposable = ProgressDialog.create(R.string.user_info_in_progress)
                .show(getActivity());

        // Structure for decoupling the message send callback that is processed upon arrival from
        // its handler that can only be executed when the app is in the foreground. Callback.
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
}

