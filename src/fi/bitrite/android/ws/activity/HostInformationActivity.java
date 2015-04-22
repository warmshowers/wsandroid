package fi.bitrite.android.ws.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Spanned;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import fi.bitrite.android.ws.view.FeedbackTable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.sort;

/**
 * Activity that fetches host information and shows it to the user.
 * The information is retrieved either from the device storage (for starred hosts)
 * or downloaded from the WarmShowers web service.
 */
public class HostInformationActivity extends WSBaseActivity
        implements android.widget.AdapterView.OnItemClickListener {

    public static final int RESULT_SHOW_HOST_ON_MAP = RESULT_FIRST_USER + 1;

    ImageView imgMemberPhoto;
    TextView lblMemberName;
    TextView txtLoginInfo;
    TextView txtHostLimitations;
    TextView txtHostLocation;
    TextView txtPhone;
    TextView txtHostComments;
    TextView txtFeedbackLabel;

    ImageView iconAvailableStatus;
    FeedbackTable feedbackTable;
    TextView comments;
    TextView txtHostServices;
    TextView txtNearbyServices;

    StarredHostDao starredHostDao = new StarredHostDaoImpl();

    private HostInformation hostInfo;
    private boolean forceUpdate;
    private HostInformationTask hostInfoTask;
    private DialogHandler dialogHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.host_information);
        initView();

        imgMemberPhoto = (ImageView) findViewById(R.id.memberPhoto);
        lblMemberName = (TextView) findViewById(R.id.lblMemberName);
        iconAvailableStatus = (ImageView)findViewById(R.id.iconAvailableStatus);
        txtLoginInfo = (TextView)findViewById(R.id.txtLoginInfo);
        txtHostLimitations = (TextView)findViewById(R.id.txtHostLimitations);

        txtHostLocation = (TextView)findViewById(R.id.txtHostLocation);
        txtPhone = (TextView)findViewById(R.id.txtPhone);

        txtHostComments = (TextView)findViewById(R.id.txtHostComments);


        txtFeedbackLabel = (TextView)findViewById(R.id.txtFeedbackLabel);
        feedbackTable = (FeedbackTable) findViewById(R.id.tblFeedback);
        comments = (TextView) findViewById(R.id.txtHostComments);
        txtHostServices = (TextView) findViewById(R.id.txtHostServices);
        txtNearbyServices = (TextView)findViewById(R.id.txtNearbyServices);

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

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        String fullname = hostInfo.getHost().getFullname();
        lblMemberName.setText(fullname);

        // Host Availability:
        // TODO: Copied from HostListAdapter.java, needs to be refactored
        // Set the host icon to black if they're available, otherwise gray
        if (host.isNotCurrentlyAvailable()) {
            iconAvailableStatus.setImageResource(R.drawable.ic_home_variant_grey600_24dp);
            iconAvailableStatus.setAlpha(0.5f);
            fullname += " " + getString(R.string.host_not_currently_available);
        } else {
            iconAvailableStatus.setImageResource(R.drawable.ic_home_variant_black_24dp);
            iconAvailableStatus.setAlpha(1.0f);
        }

        String activeDate = host.getLastLogin();
        String createdDate = host.getMemberSince();

        String memberString = getString(R.string.search_host_summary, createdDate, activeDate);
        txtLoginInfo.setText(memberString);

        String limitations = getString(R.string.host_limitations, host.getMaxCyclists(), host.getLanguagesSpoken());
        txtHostLimitations.setText(limitations);


        // Host location section
        txtHostLocation.setText(host.getLocation());
        String phones = "";
        if (!host.getMobilePhone().isEmpty()) {
            phones += getString(R.string.mobile_phone_abbrev, host.getMobilePhone()) + " ";
        }
        if (!host.getHomePhone().isEmpty()) {
            phones += getString(R.string.home_phone_abbrev, host.getHomePhone())  + " ";
        }
        if (!host.getWorkPhone().isEmpty()) {
            phones += getString(R.string.work_phone_abbrev, host.getWorkPhone())  + " ";
        }
        if (!phones.isEmpty()) {
            txtPhone.setText(phones);
            Linkify.addLinks(txtPhone, Linkify.ALL);
        }

        // Profile/Comments section
        // Allow such TextView html as it will; but Drupal's text assumes linefeeds break lines
        Spanned comments = Tools.siteHtmlToHtml(host.getComments());
        txtHostComments.setText(comments);

        // Host Services
        String hostServices = host.getHostServices(this);
        if (!hostServices.isEmpty()) {
            txtHostServices.setText(getString(R.string.host_services_description, hostServices));
        } else {
            txtHostServices.setVisibility(View.GONE);
        }

        // Nearby Services
        String nearbyServices = host.getNearbyServices(this);
        if (!nearbyServices.isEmpty()) {
            txtNearbyServices.setText(getString(R.string.nearby_services_description, nearbyServices));
        } else {
            txtNearbyServices.setVisibility(View.GONE);
        }

        List<Feedback> feedback = hostInfo.getFeedback();
        sort(feedback);
        feedbackTable.addRows(feedback);
        if (feedback.size() > 0) {
            txtFeedbackLabel.setText(getString(R.string.feedback, feedback.size()));
        } else {
            txtFeedbackLabel.setText(R.string.no_feedback_yet);
        }

        // If we're connected and there is a picture, get host picture.
        // TODO: Consider saving the picture in the db.
        if (Tools.isNetworkConnected(this)) {
            String url = profilePicture(host.getPicture());
            if (!url.isEmpty()) {
                new DownloadImageTask(imgMemberPhoto)
                        .execute(url);
            } else {
                imgMemberPhoto.setVisibility(View.GONE);
                lblMemberName.setTextColor(Color.BLACK);
                lblMemberName.setTextSize(24);
            }
        }

    }

    /**
     * Choose the variant of a profile picture to use.
     * Unfortunately this is dependent on knowing how imagecache is configured on the server.
     *
     * @param basePicture This is the picture returned by the site, like 'files/pictures/picture-1165.jpg'
     * @return Either a string with the full URL to the picture or an empty string if no picture exists
     */
    public String profilePicture(String basePicture) {
        String[] parts = basePicture.split("/", 2);
        String url = "";

        if (!basePicture.isEmpty() && parts.length == 2) {
            url = GlobalInfo.warmshowersBaseUrl + "/" + parts[0] + "/imagecache/mobile_photo_4x3/" + parts[1];
        }
        return url;
    }

    /**
     * Download an image into a bitmap in an AsyncTask
     * <p/>
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
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                bmImage.setImageBitmap(result);
                Tools.scaleImage(bmImage, bmImage.getWidth());
                // Attempt to now force the name on top of the picture
                lblMemberName.setTextColor(Color.WHITE);
            }
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
        return true; // Do not call super, as we don't want the default options menu...
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menuSendMessageIcon:
                contactHost(null);
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

