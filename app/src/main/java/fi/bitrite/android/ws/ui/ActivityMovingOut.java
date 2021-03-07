package fi.bitrite.android.ws.ui;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
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

        TextView didYouKnow = findViewById(R.id.moving_out_did_you_know);
        didYouKnow.setText(getFormattedText(getString(R.string.moving_out_did_you_know_more)));
        didYouKnow.setMovementMethod(LinkMovementMethod.getInstance());

        TextView didYouKnowDetails = findViewById(R.id.moving_out_did_you_know_details);
        didYouKnowDetails.setText(getFormattedText(getString(R.string.moving_out_did_you_know_details)));
        didYouKnowDetails.setMovementMethod(LinkMovementMethod.getInstance());

        didYouKnowDetails.setVisibility(View.GONE);
        didYouKnow.setOnClickListener(view -> {
            if (didYouKnowDetails.isShown()) {
                didYouKnowDetails.setVisibility(View.GONE);
                didYouKnow.setText(getFormattedText(getString(R.string.moving_out_did_you_know_more)));
            } else {
                didYouKnowDetails.setVisibility(View.VISIBLE);
                didYouKnow.setText(getFormattedText(getString(R.string.moving_out_did_you_know_less)));
            }
        });

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
