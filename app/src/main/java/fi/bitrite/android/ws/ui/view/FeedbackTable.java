package fi.bitrite.android.ws.ui.view;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Feedback;

/**
 * Custom table that creates rows dynamically.
 */
// TODO(saemy): Replace by ListView
public class FeedbackTable extends TableLayout {

    public FeedbackTable(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRows(List<Feedback> feedback) {
        removeAllViews();

        int i = 0;
        for (Feedback f : feedback) {

            TableRow tr = getFeedbackRow();
            TextView rating = getFeedbackText(getRowHeaderString(f));
            rating.setTypeface(null, Typeface.ITALIC);
            rating.setTextSize(16);
            rating.setPadding(0, 5, 0, 0);
            tr.addView(rating);
            addView(tr);

            tr = getFeedbackRow();
            TextView meta = getFeedbackText(getAuthorString(f));
            meta.setTypeface(null, Typeface.ITALIC);
            tr.addView(meta);
            addView(tr);

            tr = getFeedbackRow();
            TextView body = getFeedbackText(f.body);
            body.setPadding(0, 0, 0, 5);
            tr.addView(body);
            addView(tr);
        }
    }

    private String getRowHeaderString(Feedback f) {
        StringBuilder sb = new StringBuilder();

        if (!isInEditMode()) {
            sb.append(getContext().getString(getRelationStringRes(f.relation)));

            // Present hosted date without DOM because we don't carry that.
            Date meetingDate = f.meetingDate;
            if (meetingDate != null) {
                sb.append(" (");
                sb.append(formatMeetingDate(meetingDate));
                sb.append(')');
            }

            sb.append(" - ");
            sb.append(getContext().getString(getRatingStringRes(f.rating)));
        }
        return sb.toString();
    }

    private String getAuthorString(Feedback f) {
        return TextUtils.isEmpty(f.senderFullname)
                ? "Unknown" // TODO(saemy): Move into a string resource
                : f.senderFullname;
    }

    private TextView getFeedbackText(String text) {
        TextView row = new TextView(getContext());
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        row.setText(Html.fromHtml(text));
        return row;
    }

    private TableRow getFeedbackRow() {
        TableRow tr = new TableRow(getContext());
        tr.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        return tr;
    }

    private String formatMeetingDate(@Nullable Date meetingDate) {
        return meetingDate != null
                ? DateUtils.formatDateTime(getContext(), meetingDate.getTime(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                | DateUtils.FORMAT_NO_MONTH_DAY)
                : "";
    }

    @StringRes
    private static int getRelationStringRes(Feedback.Relation relation) {
        switch (relation) {
            case Host: return R.string.feedback_relation_host;
            case Guest: return R.string.feedback_relation_guest;
            case MetWhileTraveling: return R.string.feedback_relation_met_while_traveling;
            case Other: return R.string.feedback_relation_other;

            default:
                throw new RuntimeException("Unknown relation type.");
        }
    }

    @StringRes
    private static int getRatingStringRes(Feedback.Rating rating) {
        switch (rating) {
            case Positive: return R.string.feedback_rating_positive;
            case Neutral: return R.string.feedback_rating_neutral;
            case Negative: return R.string.feedback_rating_negative;

            default:
                throw new RuntimeException("Unknown rating type.");
        }
    }
}
