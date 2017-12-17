package fi.bitrite.android.ws.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
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

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.host.impl.HttpHostFeedback;
import fi.bitrite.android.ws.host.impl.HttpHostInformation;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;
import fi.bitrite.android.ws.ui.util.NavigationController;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.ui.view.FeedbackTable;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.Tools;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Activity that fetches host information and shows it to the user.
 * The information is retrieved either from the device storage (for starred hosts)
 * or downloaded from the WarmShowers web service.
 */
public class UserFragment extends BaseFragment {
    public static final String TAG = "UserFragment";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER = "user";
    private static final String KEY_FEEDBACK = "feedback";

    @Inject NavigationController mNavigationController;
    @Inject AuthenticationController mAuthenticationController;

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

    private StarredHostDao mFavoriteUsersDao = new StarredHostDaoImpl();

    private ProgressDialog.Disposable mDownloadUserInfoProgressDisposable;

    private int mUserId;

    private final BehaviorSubject<UserInformation> mUserInfo =
            BehaviorSubject.createDefault(new UserInformation());
    private final BehaviorSubject<Boolean> mFavorite = BehaviorSubject.create();
    private CompositeDisposable mDisposables;

    private UserInformationTask mUserInfoTask;

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

        mFavoriteUsersDao.open();

        if (savedInstanceState == null) {
            // Called from another activity.
            Bundle arguments = getArguments();

            mUserId = arguments.getInt(KEY_USER_ID);

            downloadUserInformation();
        } else {
            // Recovering from e.g. screen rotation change.
            mUserId = savedInstanceState.getInt(KEY_USER_ID);

            Host user = savedInstanceState.getParcelable(KEY_USER);
            List<Feedback> feedback = savedInstanceState.getParcelableArrayList(KEY_FEEDBACK);

            UserInformation userInformation = new UserInformation(user, feedback);
            mUserInfo.onNext(new UserInformation(user, feedback));

            if (mDownloadUserInfoProgressDisposable != null || user == null) {
                // We were in the process of downloading host info, retry
                downloadUserInformation();
            } else {
                updateViewContent(userInformation);
            }
        }

        mFavorite.onNext(mFavoriteUsersDao.isHostStarred(mUserId));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mFavoriteUsersDao.isOpen()) {
            mFavoriteUsersDao.open();
        }

        mDisposables = new CompositeDisposable();
        mDisposables.add(mUserInfo.subscribe(userInformation -> {
            saveUserIfFavorite(userInformation);

            updateViewContent(userInformation);
        }));

        mDisposables.add(mFavorite.subscribe(isFavorite -> {
            saveUserIfFavorite(mUserInfo.getValue());

            mCkbFavorite.setChecked(isFavorite);
            mImgFavorite.setColorFilter(isFavorite ? mFavoritedColor : mNonFavoritedColor);
        }));
    }

    private void saveUserIfFavorite(UserInformation userInformation) {
        if (!userInformation.isValid()) {
            return;
        }

        Host user = userInformation.user;
        List<Feedback> feedback = userInformation.feedback;

        if (mFavorite.getValue()) {
            mFavoriteUsersDao.update(user.getId(), user.getName(), user, feedback);
        } else {
            mFavoriteUsersDao.delete(user.getId(), user.getName());
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        UserInformation userInfo = mUserInfo.getValue();
        outState.putInt(KEY_USER_ID, mUserId);
        if (userInfo.isValid()) {
            outState.putParcelable(KEY_USER, userInfo.user);
            outState.putParcelableArrayList(KEY_FEEDBACK, new ArrayList<>(userInfo.feedback));
        }

        if (mUserInfoTask != null) {
            mUserInfoTask.cancel(false);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        mDisposables.dispose();
        mFavoriteUsersDao.close();
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

    private void updateViewContent(UserInformation userInfo) {
        mLayoutDetails.setVisibility(userInfo.isValid() ? View.VISIBLE : View.GONE);

        if (!userInfo.isValid()) {
            return;
        }

        final Host user = userInfo.user;

        mLblName.setText(user.getFullname());
        setTitle(user.getFullname());

        // Host Availability:
        // TODO: Copied from UserListAdapter.java, needs to be refactored
        // Set the user icon to black if they're available, otherwise gray
        boolean isAvailable = !user.isNotCurrentlyAvailable();
        mImgAvailability.setAlpha(isAvailable ? 1.0f : 0.5f);
        mImgAvailability.setImageResource(isAvailable
                ? R.drawable.ic_home_variant_black_24dp
                : R.drawable.ic_home_variant_grey600_24dp);
        mLblAvailability.setText(isAvailable
                ? R.string.currently_available
                : R.string.not_currently_available);

        DateFormat simpleDate = DateFormat.getDateInstance();
        String activeDate = simpleDate.format(user.getLastLoginAsDate());
        String createdDate = new SimpleDateFormat("yyyy").format(user.getCreatedAsDate());

        mLblLoginInfo.setText(getString(R.string.search_host_summary, createdDate, activeDate));
        mLblLimitations.setText(getString(
                R.string.host_limitations, user.getMaxCyclists(), user.getLanguagesSpoken()));


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

        List<Feedback> feedback = mUserInfo.getValue().feedback;
        Collections.sort(feedback);
        mTblFeedback.addRows(feedback);
        mLblFeedback.setText(feedback.isEmpty()
                ? getString(R.string.no_feedback_yet)
                : getString(R.string.feedback, feedback.size()));

        // If we're connected and there is a picture, get host picture.
        String url = user.getProfilePictureLarge();
        if (!TextUtils.isEmpty(url)) {
            Picasso.with(getContext())
                    .load(url)
                    .placeholder(R.drawable.default_hostinfo_profile)
                    .into(mImgPhoto);
        }
    }

    private void contactHost(@NonNull Host user) {
        mNavigationController.navigateToContactUser(user);
    }

    private void sendFeedback(@NonNull Host user) {
        mNavigationController.navigateToFeedback(user);
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

    private void downloadUserInformation() {
        mDownloadUserInfoProgressDisposable = ProgressDialog.create(R.string.host_info_in_progress)
                .show(getActivity());

        mUserInfoTask = new UserInformationTask(mUserId);
        mUserInfoTask.execute();
    }

    private class UserInformationTask extends AsyncTask<Void, Void, Object> {
        private final int mUserId;

        UserInformationTask(int userId) {
            mUserId = userId;
        }

        @Override
        protected Object doInBackground(Void... params) {
            try {
                if (Tools.isNetworkConnected(getContext())) {
                    // Download if we have a network connection.
                    HttpHostInformation httpHostInfo = new HttpHostInformation(mAuthenticationController);
                    HttpHostFeedback hostFeedback = new HttpHostFeedback(mAuthenticationController);

                    Host user = httpHostInfo.getHostInformation(mUserId);
                    ArrayList<Feedback> feedback = hostFeedback.getFeedback(mUserId);

                    return new UserInformation(user, feedback);
                } else {
                    // Try loading from db if favorite and no network.
                    Host host = mFavoriteUsersDao.getHost(mUserId);
                    if (host != null) {
                        List<Feedback> feedback =
                                mFavoriteUsersDao.getFeedback(mUserId, host.getName());
                        return new UserInformation(host, feedback);
                    }
                }

                return new UserInformation();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                return e;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            mDownloadUserInfoProgressDisposable.dispose();
            mDownloadUserInfoProgressDisposable = null;

            if (result instanceof Exception) {
                RestClient.reportError(getContext(), result);
                return;
            }

            mUserInfo.onNext((UserInformation) result);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.host_information_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Host user = mUserInfo.getValue().user;
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

    static class UserInformation {
        public final Host user;
        public final List<Feedback> feedback;

        UserInformation() {
            this(null, null);
        }
        UserInformation(Host user, List<Feedback> feedback) {
            this.user = user;
            this.feedback = feedback == null ? new ArrayList<>() : feedback;
        }

        boolean isValid() {
            return user != null;
        }
    }
}

