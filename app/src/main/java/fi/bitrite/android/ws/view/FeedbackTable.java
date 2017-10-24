package fi.bitrite.android.ws.view;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.util.ArrayTranslator;

/**
 * Custom table that creates rows dynamically.
 */
public class FeedbackTable extends TableLayout {

    ArrayTranslator translator;
    Context mContext;

    public FeedbackTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        if (!isInEditMode()) {
            translator = ArrayTranslator.getInstance();
        }
    }

    public void addRows(List<Feedback> feedback) {

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
            TextView body = getFeedbackText(f.getBody());
            body.setPadding(0, 0, 0, 5);
            tr.addView(body);
            addView(tr);
        }
    }

    private String getRowHeaderString(Feedback f) {
        StringBuilder sb = new StringBuilder();

        if (!isInEditMode()) {
            sb.append(translator.translateHostGuest(f.getGuestOrHostStr()));

            // Present hosted date without DOM because we don't carry that.
            Date hostingDate = f.getHostingDate();
            if (hostingDate != null) {
                int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY;
                String hostedOn = DateUtils.formatDateTime(mContext, hostingDate.getTime(), flags);

                sb.append(" (");
                sb.append(hostedOn);
                sb.append(')');
            }

            sb.append(" - ");
            sb.append(translator.translateRating(f.getRatingStr()));
        }
        return sb.toString();
    }

    private String getAuthorString(Feedback f) {
        String name = f.getFullname();
        if (name == null || name.length() == 0) {
            name = "Unknown";
        }
        return name;
    }

    private TextView getFeedbackText(String text) {
        TextView row = new TextView(getContext());
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        row.setText(text);
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
}
