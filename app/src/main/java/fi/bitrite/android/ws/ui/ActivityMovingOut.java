package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import fi.bitrite.android.ws.R;

/**
 * The activity responsible for getting Warmshowers credentials from the user,
 * verifying them against the Warmshowers web service and storing
 * them on the device using Android's custom account facilities.
 */
public class ActivityMovingOut extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_moving_out);
        TextView tv = findViewById(R.id.moving_out_title_description);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
