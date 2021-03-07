package fi.bitrite.android.ws.ui;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import fi.bitrite.android.ws.R;

/**
 * The activity displays goodbye message to the user.
 */
public class ActivityMovingOut extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_moving_out);
        TextView message = findViewById(R.id.moving_out_message);
        message.setText(getFormattedText(getString(R.string.moving_out_message)));
        message.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private Spanned getFormattedText(String text) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT) :
                Html.fromHtml(text);
    }
}
