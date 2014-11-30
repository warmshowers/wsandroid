package fi.bitrite.android.ws.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.inject.Inject;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.activity.model.HostInformation;
import fi.bitrite.android.ws.host.impl.HttpHostFeedback;
import fi.bitrite.android.ws.host.impl.HttpHostInformation;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.http.HttpException;
import fi.bitrite.android.ws.view.FeedbackTable;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.sort;

/**
 * Activity that fetches host information and shows it to the user.
 * The information is retrieved either from the device storage (for starred hosts)
 * or downloaded from the WarmShowers web service.
 */
public class HostInformationActivity extends RoboActivity {

    public static final int RESULT_SHOW_HOST_ON_MAP = RESULT_FIRST_USER + 1;

    @InjectView(R.id.layoutHostDetails)
    LinearLayout hostDetails;

    @InjectView(R.id.scrollHostInformation)
    ScrollView hostInformationScroller;

    @InjectView(R.id.btnToggleBasicInformation)
    ImageView basicInformationExpander;
    @InjectView(R.id.tableBasicInformation)
    TableLayout basicInformation;

    @InjectView(R.id.lblFeedback)
    TextView feedbackLabel;
    @InjectView(R.id.btnToggleFeedback)
    ImageView feedbackExpander;
    @InjectView(R.id.tblFeedback)
    FeedbackTable feedbackTable;

    @InjectView(R.id.btnHostStar)
    ImageView star;
    @InjectView(R.id.txtHostFullname)
    TextView fullname;
    @InjectView(R.id.txtHostComments)
    TextView comments;
    @InjectView(R.id.txtMemberSince)
    TextView memberSince;
    @InjectView(R.id.txtLastLogin)
    TextView lastLogin;
    @InjectView(R.id.txtViewOnSite)
    TextView viewOnSite;
    @InjectView(R.id.txtLeaveFeedback)
    TextView leaveFeedback;

    @InjectView(R.id.txtHostLocation)
    TextView location;
    @InjectView(R.id.txtHostMobilePhone)
    TextView mobilePhone;
    @InjectView(R.id.txtHostHomePhone)
    TextView homePhone;
    @InjectView(R.id.txtHostWorkPhone)
    TextView workPhone;
    @InjectView(R.id.txtPreferredNotice)
    TextView preferredNotice;
    @InjectView(R.id.txtMaxGuests)
    TextView maxGuests;
    @InjectView(R.id.txtNearestAccomodation)
    TextView nearestAccomodation;
    @InjectView(R.id.txtCampground)
    TextView campground;
    @InjectView(R.id.txtBikeShop)
    TextView bikeShop;
    @InjectView(R.id.txtServices)
    TextView services;

    @Inject
    StarredHostDao starredHostDao;

    private HostInformation hostInfo;
    private boolean forceUpdate;
    private HostInformationTask hostInfoTask;
    private DialogHandler dialogHandler;
    private static String TAG = "HostInformationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.host_information);

        dialogHandler = new DialogHandler(HostInformationActivity.this);
        boolean inProgress = DialogHandler.inProgress();
        boolean shouldDownloadHostInfo;
        forceUpdate = false;

        starredHostDao.close();
        starredHostDao.open();

        if (savedInstanceState != null) {
            // recovering from e.g. screen rotation change
            hostInfo = HostInformation.fromSavedInstanceState(savedInstanceState, starredHostDao);
            forceUpdate = savedInstanceState.getBoolean("force_update");
            shouldDownloadHostInfo = inProgress;
        } else {
            // returning from another activity
            Intent i = getIntent();
            hostInfo = HostInformation.fromIntent(i, starredHostDao);

            if (intentProvidesFullHostInfo(i)) {
                shouldDownloadHostInfo = false;
            } else {
                if (hostInfo.isStarred()) {
                    hostInfo.setHost(starredHostDao.getHost(hostInfo.getId()));
                    hostInfo.setFeedback(starredHostDao.getFeedback(hostInfo.getId(), hostInfo.getHost().getName()));
                    forceUpdate = i.getBooleanExtra("update", false);
                    shouldDownloadHostInfo = forceUpdate;
                } else {
                    shouldDownloadHostInfo = true;
                }
            }
        }

        starredHostDao.close();

        if (shouldDownloadHostInfo) {
            downloadHostInformation();
        } else {
            updateViewContent();
        }

        setupStar();

        fullname.setText(hostInfo.getHost().getFullname());
    }

    private boolean intentProvidesFullHostInfo(Intent i) {
        return i.getBooleanExtra("full_info", false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        hostInfo.onSaveInstanceState(outState);
        outState.putBoolean("force_update", forceUpdate);

        if (hostInfoTask != null) {
            hostInfoTask.cancel(false);
        }

        super.onSaveInstanceState(outState);
    }

    private void setupStar() {
        int drawable = hostInfo.isStarred() ? R.drawable.starred_on : R.drawable.starred_off;
        star.setImageDrawable(getResources().getDrawable(drawable));
        star.setVisibility(View.VISIBLE);
    }

    public void showStarHostDialog(View view) {
        toggleHostStarred();
        int msgId = (hostInfo.isStarred() ? R.string.host_starred : R.string.host_unstarred);
        Toast.makeText(this, getResources().getString(msgId), Toast.LENGTH_LONG).show();
    }

    protected void toggleHostStarred() {
        starredHostDao.open();

        if (starredHostDao.isHostStarred(hostInfo.getId(), hostInfo.getHost().getName())) {
            starredHostDao.delete(hostInfo.getId(), hostInfo.getHost().getName());
        } else {
            starredHostDao.insert(hostInfo.getId(), hostInfo.getHost().getName(), hostInfo.getHost(), hostInfo.getFeedback());
        }

        hostInfo.toggleStarred();
        setupStar();
        starredHostDao.close();
    }

    public void toggleBasicInformation(View view) {
        if (basicInformation.getVisibility() == View.GONE) {
            basicInformation.setVisibility(View.VISIBLE);
            basicInformationExpander.setImageDrawable(getResources().getDrawable(R.drawable.expander_max));
        } else {
            basicInformation.setVisibility(View.GONE);
            basicInformationExpander.setImageDrawable(getResources().getDrawable(R.drawable.expander_min));
        }
    }

    public void toggleFeedback(View view) {
        if (feedbackTable.getVisibility() != View.GONE) {
            feedbackTable.setVisibility(View.GONE);
            feedbackExpander.setImageDrawable(getResources().getDrawable(R.drawable.expander_min));
        } else {
            feedbackTable.setVisibility(View.VISIBLE);
            feedbackExpander.setImageDrawable(getResources().getDrawable(R.drawable.expander_max));
            hostInformationScroller.post(new Runnable() {
                public void run() {
                    hostInformationScroller.scrollTo(0, hostInformationScroller.getBottom());
                }
            });
        }
    }

    public void contactHost(View view) {
        Intent i = new Intent(HostInformationActivity.this, HostContactActivity.class);
        i.putExtra("host", hostInfo.getHost());
        i.putExtra("id", hostInfo.getId());
        startActivity(i);
    }

    public void showHostOnMap(View view) {
        // We need to finish the host info dialog, switch to the map tab and
        // zoom/scroll to the location of the host

        Intent intent = new Intent(this, Maps2Activity.class);

        intent.putExtra("target_map_latlng", (Parcelable) hostInfo.getHost().getLatLng());

        startActivity(intent);

//        // #31: when going back from the map, we should end up on the host info page
//        hostInfo.saveInIntent(intent);
//
//        setResult(RESULT_SHOW_HOST_ON_MAP, intent);
//        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogHandler.inProgress()) {
            return dialogHandler.createDialog(id, getResources().getString(R.string.host_info_in_progress));
        } else {
            return null;
        }
    }

    private void downloadHostInformation() {
        dialogHandler.showDialog(DialogHandler.HOST_INFORMATION);
        hostInfoTask = new HostInformationTask();
        hostInfoTask.execute();
    }

    private void updateViewContent() {
        final Host host = hostInfo.getHost();

        String availability = host.getNotCurrentlyAvailable().equals("1") ? getString(R.string.host_not_currently_available) : getString(R.string.host_currently_available);
        // Allow such TextView html as it will; but Drupal's text assumes linefeeds break lines
        comments.setText(Tools.siteHtmlToHtml(availability + "<br/>" + host.getComments()));

        location.setText(host.getLocation());
        memberSince.setText(host.getMemberSince());
        lastLogin.setText(host.getLastLogin());
        mobilePhone.setText(host.getMobilePhone());
        homePhone.setText(host.getHomePhone());
        workPhone.setText(host.getWorkPhone());
        preferredNotice.setText(host.getPreferredNotice());
        maxGuests.setText(host.getMaxCyclists());
        nearestAccomodation.setText(host.getMotel());
        campground.setText(host.getCampground());
        bikeShop.setText(host.getBikeshop());
        services.setText(host.getServices());

        // Set up to view the member account on warmshowers.org
        viewOnSite.setText(Html.fromHtml("<u>" + getString(R.string.view_on_site) + "</u>"));
        viewOnSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = GlobalInfo.warmshowersBaseUrl + "/user/" + hostInfo.getId();
                WebViewActivity.viewOnSite(HostInformationActivity.this, url, host.getFullname());
            }
        });

        // This experimental hack at adding the ability to leave feedback is optional. We might
        // just try it and see if it works. I'd rather replace it with a "real" anddroid function,
        // but it was just sitting here as a freebie as I was working on providing access to the site
        // via webview. rfay 2014-11-25
        leaveFeedback.setText(Html.fromHtml("<u>" + getString(R.string.leave_feedback) + "</u>"));
        leaveFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = GlobalInfo.warmshowersBaseUrl + "/node/add/trust-referral?edit%5Bfield_member_i_trust%5D%5B0%5D%5Buid%5D%5Buid%5D=" + host.getName();
                WebViewActivity.viewOnSite(HostInformationActivity.this, url, getString(R.string.leave_feedback_for, host.getFullname()));
            }
        });

        List<Feedback> feedback = hostInfo.getFeedback();
        sort(feedback);
        feedbackTable.addRows(feedback);
        feedbackLabel.setText(getResources().getString(R.string.feedback) + " (" + feedback.size() + ")");

        hostDetails.setVisibility(View.VISIBLE);

        if (hostInfo.getHost().isNotCurrentlyAvailable()) {
            dialogHandler.alert(getResources().getString(R.string.host_not_available));
        }

    }

    private class HostInformationTask extends AsyncTask<Void, Void, Object> {

        @Override
        protected Object doInBackground(Void... params) {
            Object retObj = null;

            try {
                HttpHostInformation httpHostInfo = new HttpHostInformation();
                HttpHostFeedback hostFeedback = new HttpHostFeedback();
                int uid = hostInfo.getId();

                Host host = httpHostInfo.getHostInformation(uid);
                ArrayList<Feedback> feedback = hostFeedback.getFeedback(uid);
                hostInfo = new HostInformation(host, feedback, uid, false);

            } catch (Exception e) {
                Log.e(WSAndroidApplication.TAG, e.getMessage(), e);
                retObj = e;
            }

            return retObj;
        }

        @Override
        protected void onPostExecute(Object result) {
            dialogHandler.dismiss();

            if (result instanceof Exception) {
                // TODO: Improve error reporting with more specifics
                int r = (result instanceof HttpException ? R.string.network_error : R.string.error_retrieving_host_information);
                dialogHandler.alert(getResources().getString(r));
                return;
            }

            updateViewContent();

            if (hostInfo.isStarred() && forceUpdate) {
                starredHostDao.open();
                starredHostDao.update(hostInfo.getId(), hostInfo.getHost().getName(), hostInfo.getHost(), hostInfo.getFeedback());
                starredHostDao.close();
                dialogHandler.alert(getResources().getString(R.string.host_updated));
            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.host_information_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuMap:
                showHostOnMap(null);
                return true;
            case R.id.menuStar:
                showStarHostDialog(null);
                return true;
            case R.id.menuUpdate:
                Intent i = new Intent();
                i.putExtra("host", hostInfo.getHost());
                i.putExtra("id", hostInfo.getId());
                i.putExtra("update", true);
                setIntent(i);
                onCreate(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

}

