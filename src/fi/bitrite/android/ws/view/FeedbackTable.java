package fi.bitrite.android.ws.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import fi.bitrite.android.ws.model.Feedback;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Custom table that creates rows dynamically.
 */
public class FeedbackTable extends TableLayout {
    public FeedbackTable(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void addRows(List<Feedback> feedback) {

        int i = 0;
        for (Feedback f : feedback) {
            int bgColor = (i++ % 2 == 0) ? 0 : 0xFF222222;

            TableRow tr = getFeedbackRow(bgColor);

            TextView meta = getFeedbackText(getMetaString(f));
            meta.setTypeface(null, Typeface.ITALIC);
            meta.setPadding(0, 5, 0, 0);
            tr.addView(meta);
            addView(tr);

            tr = getFeedbackRow(bgColor);
            TextView rating = getFeedbackText(f.getRating());
            rating.setTypeface(null, Typeface.ITALIC);
            tr.addView(rating);
            addView(tr);

            tr = getFeedbackRow(bgColor);
            TextView body = getFeedbackText(f.getBody());
            body.setPadding(0, 0, 0, 5);
            tr.addView(body);
            addView(tr);
        }
    }

    private String getMetaString(Feedback f) {
        StringBuilder sb = new StringBuilder();
        String name = f.getFullname();
        if (name == null || name.length() == 0) {
            name = "Unknown";
        }

        sb.append(name.trim());

        sb.append(" (");
        sb.append(f.getGuestOrHost());
        sb.append(", ");
        Date d = new Date(f.getHostingDate()*1000L);
        String hostedOn = DateFormat.getDateInstance().format(d);
        sb.append(hostedOn);
        sb.append(")");
        return sb.toString();
    }

    private TextView getFeedbackText(String text) {
        TextView row = new TextView(getContext());
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.FILL_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        row.setText(text);
        return row;
    }

    private TableRow getFeedbackRow(int bgColor) {
        TableRow tr = new TableRow(getContext());
        tr.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.FILL_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        tr.setBackgroundColor(bgColor);
        return tr;
    }
}
