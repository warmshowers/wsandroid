package fi.bitrite.android.ws.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;

import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.R;


public class AboutActivity extends WSBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initView();

        TextView versionTextView = (TextView) findViewById(R.id.app_version);
        versionTextView.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));
        TextView googleDetails = (TextView) findViewById(R.id.txtAboutDetailsGoogle);
        String licenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);
        if (licenseInfo != null) {
            // licenseInfo is a bit of a mess (coming directly from google)
            // Change the multi-\n to <br/>, then change single \n perhaps followed by whitespace to a space
            // then change the <br/> back to \n
            licenseInfo = licenseInfo.replaceAll("\n\n+", "<br/>").replaceAll("\n[ \t]*", " ").replace("<br/>", "\n");
            googleDetails.setText(licenseInfo);
        }

    }

}
