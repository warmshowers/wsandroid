package fi.bitrite.android.ws.ui;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import fi.bitrite.android.ws.R;

/**
 * The activity displays Goodbye Warmshowers message to the user.
 */
public class ActivityMovingOut extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_moving_out);

        TextView message = findViewById(R.id.moving_out_message);
        message.setText(getFormattedText(getString(R.string.moving_out_message)));
        message.setMovementMethod(LinkMovementMethod.getInstance());

        TextView backstoryBody = findViewById(R.id.moving_out_backstory_body);
        backstoryBody.setText(getFormattedText(getString(R.string.moving_out_backstory_body)));
        backstoryBody.setMovementMethod(LinkMovementMethod.getInstance());

        TextView backstoryTitle = findViewById(R.id.moving_out_backstory_title);
        LinearLayout backstoryHead = findViewById(R.id.moving_out_backstory_head);
        ImageButton backstoryExpand = findViewById(R.id.moving_out_backstory_expand_collapse);
        View.OnClickListener backstoryOnClickListener =
                new BackstoryOnClickListener(backstoryBody, backstoryExpand);
        backstoryHead.setOnClickListener(backstoryOnClickListener);
        backstoryTitle.setOnClickListener(backstoryOnClickListener);
        backstoryExpand.setOnClickListener(backstoryOnClickListener);
    }

    private static class BackstoryOnClickListener implements View.OnClickListener {
        private final View body;
        private final View expandIcon;
        private final int backstoryBodyHeight;
        private final int duration = 600;
        private boolean isExpanded = false;

        BackstoryOnClickListener(View body, View expandIcon) {
            this.body = body;
            this.expandIcon = expandIcon;

            // Calculate the expanded size of the backstory body.
            int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    ((View) body.getParent()).getWidth(),
                    View.MeasureSpec.EXACTLY);
            int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    0,
                    View.MeasureSpec.UNSPECIFIED);
            body.measure(matchParentMeasureSpec, wrapContentMeasureSpec);
            backstoryBodyHeight = body.getMeasuredHeight();
        }

        @Override
        public void onClick(View view) {
            if (!isExpanded) {
                // Expand.
                isExpanded = true;
                Animation a = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        body.getLayoutParams().height = interpolatedTime == 1
                                ? LinearLayout.LayoutParams.WRAP_CONTENT
                                : (int)(backstoryBodyHeight * interpolatedTime);
                        body.requestLayout();
                    }

                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }
                };
                a.setDuration(duration);

                // Older versions of android (pre API 21) cancel animations for views with a height of 0.
                body.getLayoutParams().height = 1;
                body.setVisibility(View.VISIBLE);
                body.startAnimation(a);

                expandIcon.animate()
                        .rotation(180f)
                        .setDuration(duration)
                        .start();
            } else {
                // Collapse.
                isExpanded = false;
                Animation a = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        if(interpolatedTime == 1){
                            body.setVisibility(View.GONE);
                        }else{
                            body.getLayoutParams().height =
                                    (int)((1 - interpolatedTime) * backstoryBodyHeight);
                            body.requestLayout();
                        }
                    }

                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }
                };
                a.setDuration(duration);
                body.startAnimation(a);

                expandIcon.animate()
                        .rotation(0f)
                        .setDuration(duration)
                        .start();
            }
        }
    }

    private Spanned getFormattedText(String text) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT) :
                Html.fromHtml(text);
    }
}
