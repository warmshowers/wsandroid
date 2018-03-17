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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

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
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.repository.FavoriteRepository;
import fi.bitrite.android.ws.repository.FeedbackRepository;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.util.NavigationController;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.ui.view.FeedbackTable;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.MaybeNull;
import fi.bitrite.android.ws.util.Tools;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

import static android.graphics.PorterDuff.Mode.MULTIPLY;

/**
 * Activity that fetches host information and shows it to the user.
 * The information is retrieved either from the device storage (for starred hosts)
 * or downloaded from the WarmShowers web service.
 */
public class UserFragment extends BaseFragment {
    public static final String TAG = "UserFragment";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER = "user";
    private final BehaviorSubject<MaybeNull<Host>> mUser =
            BehaviorSubject.createDefault(new MaybeNull<>());
    private final BehaviorSubject<List<Feedback>> mFeedbacks = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> mFavorite = BehaviorSubject.create();
    @Inject NavigationController mNavigationController;
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
    private ProgressDialog.Disposable mDownloadUserInfoProgressDisposable;
    private int mUserId;
    private CompositeDisposable mDisposables;

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

        getUserInformation();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mDisposables = new CompositeDisposable();
        mDisposables.add(mUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> updateUserViewContent()));

        mDisposables.add(mFeedbacks
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(feedbacks -> updateFeedbacksViewContent()));

        mDisposables.add(mFavorite
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isFavorite -> {
                    saveUserIfFavorite();

                    mCkbFavorite.setChecked(isFavorite);
                    mImgFavorite.setColorFilter(isFavorite ? mFavoritedColor : mNonFavoritedColor);
                }));
    }

    private void saveUserIfFavorite() {
        boolean isFavorite = mFavorite.getValue();
        if (isFavorite == mDbFavoriteStatus) {
            return;
        }
        if (mUser.getValue().isNull()) {
            return;
        }

        Host user = mUser.getValue().data;
        List<Feedback> feedback = mFeedbacks.getValue();

        if (isFavorite) {
            mFavoriteRepository.add(user, feedback);
        } else {
            mFavoriteRepository.remove(user.getId());
        }

        mDbFavoriteStatus = isFavorite;
    }

    @Override
    public void onPause() {
        mDisposables.dispose();
        super.onPause();
    }

    @OnCheckedChanged(R.id.user_ckb_favorite)
    void onFavoriteSelected(CompoundButton button, boolean isChecked) {
        if (mFavorite.getValue() == isChecked) {
            return;
        }

        mFavorite.onNext(isChecked);

        int msgId = isChecked
                ? R.string.host_starred
                : R.string.host_unstarred;
        Toast.makeText(getContext(), msgId, Toast.LENGTH_SHORT).show();
    }

    private void updateUserViewContent() {
        MaybeNull<Host> maybeUser = mUser.getValue();
        mLayoutDetails.setVisibility(maybeUser.isNonNull() ? View.VISIBLE : View.GONE);

        if (maybeUser.isNull()) {
            return;
        }

        final Host user = maybeUser.data;

        mLblName.setText(user.getFullname());
        setTitle(user.getFullname());

        // Host Availability:
        // Set the user icon to black if they're available, otherwise gray
        boolean isAvailable = !user.isNotCurrentlyAvailable();
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
        String activeDate = simpleDate.format(user.getLastLoginAsDate());
        String createdDate = new SimpleDateFormat("yyyy").format(user.getCreatedAsDate());

        mLblLoginInfo.setText(getString(R.string.search_host_summary, createdDate, activeDate));
        mLblLimitations.setText(getString(
                R.string.host_limitations, user.getMaxCyclists(), user.getLanguagesSpoken())
        );


        // Host location section
        mLblLocation.setText(user.getLocation());
        List<String> phones = new ArrayList<>();
        if (!user.getMobilePhone().isEmpty()) {
            phones.add(getString(R.string.mobile_phone_abbrev, user.getMobilePhone()));
        }
        if (!user.getHomePhone().isEmpty()) {
            phones.add(getString(R.string.home_phone_abbrev, user.getHomePhone()));
        }
        if (!user.getWorkPhone().isEmpty()) {
            phones.add(getString(R.string.work_phone_abbrev, user.getWorkPhone()));
        }

        mLblPhone.setVisibility(phones.isEmpty() ? View.GONE : View.VISIBLE);
        if (!phones.isEmpty()) {
            mLblPhone.setText(StringUtils.join(phones, '\n'));
            Linkify.addLinks(mLblPhone, Linkify.ALL);
        }

        // Profile/Comments section
        // Allow such TextView html as it will; but Drupal's text assumes linefeeds break lines
        mLblComments.setText(Tools.siteHtmlToHtml(user.getComments()));

        // Host Services
        String hostServices = user.getHostServices(getContext());
        mLblServices.setVisibility(hostServices.isEmpty() ? View.GONE : View.VISIBLE);
        if (!hostServices.isEmpty()) {
            mLblServices.setText(getString(R.string.host_services_description, hostServices));
        }

        // Nearby Services
        String nearbyServices = user.getNearbyServices(getContext());
        mLblNearbyServices.setVisibility(nearbyServices.isEmpty() ? View.GONE : View.VISIBLE);
        if (!nearbyServices.isEmpty()) {
            mLblNearbyServices.setText(getString(R.string.nearby_services_description, nearbyServices));
        }

        // If we're connected and there is a picture, get host picture.
        String url = user.getProfilePictureLarge();
        if (!TextUtils.isEmpty(url)) {
            Picasso.with(getContext())
                    .load(url)
                    .placeholder(R.drawable.default_hostinfo_profile)
                    .into(mImgPhoto);
            mImgPhoto.setContentDescription(getString(
                    R.string.content_description_avatar_of_var, user.getName())
            );
        }
    }

    private void updateFeedbacksViewContent() {
        List<Feedback> feedbacks = mFeedbacks.getValue();
        Collections.sort(feedbacks,
                (left, right) -> ObjectUtils.compare(right.meetingDate, left.meetingDate)
        );

        mTblFeedback.setRows(feedbacks);
        mLblFeedback.setText(feedbacks.isEmpty()
                ? getString(R.string.no_feedback_yet)
                : getString(R.string.feedback, feedbacks.size()));
    }

    private void contactHost(@NonNull Host user) {
        mNavigationController.navigateToContactUser(user);
    }

    private void sendFeedback(@NonNull Host user) {
        mNavigationController.navigateToFeedback(user.getId());
    }

    private void showHostOnMap(@NonNull Host user) {
        mNavigationController.navigateToMap(user.getLatLng());
    }

    /**
     * Send a geo intent so that we can view the host on external maps application
     */
    public void sendGeoIntent(@NonNull Host user) {
        String lat = user.getLatitude();
        String lng = user.getLongitude();
        String query = Uri.encode(lat + "," + lng + "(" + user.getFullname() + ")");
        Uri uri = Uri.parse("geo:" + lat + "," + lng + "?q=" + query);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void viewOnSite(@NonNull Host user) {
        String url = GlobalInfo.warmshowersBaseUrl + "/user/" + user.getId();
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private void getUserInformation() {
        mDownloadUserInfoProgressDisposable = ProgressDialog.create(R.string.host_info_in_progress)
                .show(getActivity());

        AtomicInteger numFinished = new AtomicInteger(0);
        Observable.merge(
                mUserRepository.get(mUserId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(hostResource -> {
                            if (hostResource.hasData()) {
                                mUser.onNext(new MaybeNull<>(hostResource.data));
                            }

                            // TODO(saemy): Error handling.
//                        if (hostResource.isError()) {
//                            RestClient.reportError(getContext(), hostResource.error);
//                        }

                            return !hostResource.isLoading();
                        }),
                mFeedbackRepository.getForRecipient(mUserId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(feedbacksResource -> {
                            if (feedbacksResource.hasData()) {
                                mFeedbacks.onNext(feedbacksResource.data);
                            }
                            // TODO(saemy): Error handling.
                            return !feedbacksResource.isLoading();
                        }))
                .subscribe(finished -> {
                    if (finished && numFinished.incrementAndGet() >= 2) {
                        mDownloadUserInfoProgressDisposable.dispose();
                    }
                }, throwable -> {
                    mDownloadUserInfoProgressDisposable.dispose();
                    HttpErrorHelper.showErrorToast(getContext(), throwable);
                });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.host_information_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Host user = mUser.getValue().data;
        if (user == null) {
            return super.onOptionsItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.menuSendMessage:
                contactHost(user);
                return true;
            case R.id.menuViewOnMap:
                showHostOnMap(user);
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
}

