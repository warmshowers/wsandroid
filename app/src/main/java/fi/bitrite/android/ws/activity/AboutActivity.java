package fi.bitrite.android.ws.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

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

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.about_actions, menu);
        return true; // Causes menu to be displayed
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_source_software:
                Intent i = new Intent(this, LegalNoticesActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
