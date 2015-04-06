package fi.bitrite.android.ws.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.*;
import com.google.android.gms.analytics.GoogleAnalytics;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.activity.model.HostInformation;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.host.impl.HttpHostFeedback;
import fi.bitrite.android.ws.host.impl.HttpHostInformation;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.http.HttpException;
import fi.bitrite.android.ws.util.http.HttpUtils;
import fi.bitrite.android.ws.view.FeedbackTable;
import roboguice.RoboGuice;
import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.InjectView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.sort;

/**
 * Activity that fetches host information and shows it to the user.
 * The information is retrieved either from the device storage (for starred hosts)
 * or downloaded from the WarmShowers web service.
 */
public class HostInformationActivity extends RoboActionBarActivity {

    public static final int RESULT_SHOW_HOST_ON_MAP = RESULT_FIRST_USER + 1;

    Menu optionsMenu;

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

    @InjectView(R.id.memberPhoto)
    ImageView memberPhoto;

    @InjectView(R.id.txtHostComments)
    TextView comments;
    @InjectView(R.id.txtMemberSince)
    TextView memberSince;
    @InjectView(R.id.txtLastLogin)
    TextView lastLogin;
    @InjectView(R.id.txtLanguagesSpoken)
    TextView languagesSpoken;

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
    @InjectView(R.id.lblMemberName)
    TextView lblMemberName;

    StarredHostDao starredHostDao = new StarredHostDaoImpl();

    private HostInformation hostInfo;
    private boolean forceUpdate;
    private HostInformationTask hostInfoTask;
    private DialogHandler dialogHandler;

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

        getSupportActionBar().setTitle(getString(R.string.hostinfo_actiivity_title));
        lblMemberName.setText(hostInfo.getHost().getFullname());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }


    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        RoboGuice.getInjector(this).injectViewMembers(this);
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

    public void showStarHostToast(View view) {
        int msgId = (hostInfo.isStarred() ? R.string.host_starred : R.string.host_unstarred);
        Toast.makeText(this, getResources().getString(msgId), Toast.LENGTH_LONG).show();
    }

    protected void toggleHostStarred() {
        toggleHostStarredOnDevice();
        setHostStarredInUI();
    }

    private void setHostStarredInUI() {
        optionsMenu.findItem(R.id.menuStarIcon).setIcon(hostInfo.isStarred() ? R.drawable.ic_action_star_on : R.drawable.ic_action_star_off);
        optionsMenu.findItem(R.id.menuStar).setTitle(getResources().getString(hostInfo.isStarred() ? R.string.unstar_this_host : R.string.star_this_host));
    }

    private void toggleHostStarredOnDevice() {
        starredHostDao.open();

        if (starredHostDao.isHostStarred(hostInfo.getId(), hostInfo.getHost().getName())) {
            starredHostDao.delete(hostInfo.getId(), hostInfo.getHost().getName());
        } else {
            starredHostDao.insert(hostInfo.getId(), hostInfo.getHost().getName(), hostInfo.getHost(), hostInfo.getFeedback());
        }

        hostInfo.toggleStarred();
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

    public void sendFeedback(View view) {
        Intent i = new Intent(HostInformationActivity.this, FeedbackActivity.class);
        i.putExtra("host", hostInfo.getHost());
        i.putExtra("id", hostInfo.getId());
        startActivity(i);
    }

    /**
     * Show host in context on our own Maps2Activity
     *
     * @param view
     */
    public void showHostOnMap(View view) {
        Intent intent = new Intent(this, Maps2Activity.class);
        intent.putExtra("target_map_latlng", (Parcelable) hostInfo.getHost().getLatLng());
        startActivity(intent);
    }

    /**
     * Send a geo intent so that we can view the host on external maps application
     *
     * @param view
     */
    public void sendGeoIntent(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String lat = hostInfo.getHost().getLatitude();
        String lng = hostInfo.getHost().getLongitude();
        String query = Uri.encode(lat + "," + lng + "(" + hostInfo.getHost().getFullname() + ")");
        String uri = "geo:" + lat + "," + lng + "?q=" + query;
        intent.setData(Uri.parse(uri));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
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
        Spanned text = Tools.siteHtmlToHtml(host.getComments() + "<br/><br/><b>" + availability + "</b>");
        comments.setText(text);

        location.setText(host.getLocation());
        memberSince.setText(host.getMemberSince());
        lastLogin.setText(host.getLastLogin());
        languagesSpoken.setText(host.getLanguagesSpoken());
        mobilePhone.setText(host.getMobilePhone());
        homePhone.setText(host.getHomePhone());
        workPhone.setText(host.getWorkPhone());
        preferredNotice.setText(host.getPreferredNotice());
        maxGuests.setText(host.getMaxCyclists());
        nearestAccomodation.setText(host.getMotel());
        campground.setText(host.getCampground());
        bikeShop.setText(host.getBikeshop());
        services.setText(host.getServices(this));

        List<Feedback> feedback = hostInfo.getFeedback();
        sort(feedback);
        feedbackTable.addRows(feedback);
        feedbackLabel.setText(getResources().getString(R.string.feedback) + " (" + feedback.size() + ")");

        hostDetails.setVisibility(View.VISIBLE);

        if (host.isNotCurrentlyAvailable()) {
            dialogHandler.alert(getResources().getString(R.string.host_not_available));
        }

        // If we're connected, get host picture.
        if (Tools.isNetworkConnected(this)) {
            String url = profilePicture(host.getPicture());
            if (!url.isEmpty()) {
                new DownloadImageTask(memberPhoto)
                        .execute(url);
            }
        }

    }

    /**
     * Choose the variant of a profile picture to use.
     * Unfortunately this is dependent on knowing how imagecache is configured on the server.
     *
     * @param basePicture
     *   This is the picture returned by the site, like 'files/pictures/picture-1165.jpg'
     *
     * @return
     *   Either a string with the full URL to the picture or an empty string if no picture exists
     */
    public String profilePicture(String basePicture) {
        String[] parts = basePicture.split("/", 2);
        String url = "";

        if (!basePicture.isEmpty() && parts.length == 2) {
            url = GlobalInfo.warmshowersBaseUrl + "/" + parts[0] + "/imagecache/mobile_profile_photo_std/" + parts[1];
        }
        return url;
    }

    /**
     * Download an image into a bitmap in an AsyncTask
     *
     * From http://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
     */
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
//                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    public void viewOnSite() {
        final Host host = hostInfo.getHost();
        String url = GlobalInfo.warmshowersBaseUrl + "/user/" + hostInfo.getId();
        WebViewActivity.viewOnSite(HostInformationActivity.this, url, host.getFullname());
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
                RestClient.reportError(HostInformationActivity.this, result);
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
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.host_information_actions, menu);
        optionsMenu = menu;
        setHostStarredInUI();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menuSendMessageIcon:
            case R.id.menuSendMessage:
                contactHost(null);
                return true;
            case R.id.menuStarIcon:
            case R.id.menuStar:
                toggleHostStarred();
                showStarHostToast(null);
                return true;
            case R.id.menuMap:
                showHostOnMap(null);
                return true;
            case R.id.menuMapApplication:
                sendGeoIntent(null);
                return true;
            case R.id.menuLeaveFeedback:
                sendFeedback(null);
                return true;
            case R.id.menuViewOnSite:
                viewOnSite();
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

