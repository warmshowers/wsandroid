package fi.bitrite.android.ws.activity;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.NavRow;

/**
 * Created by rfay on 4/19/15.
 */
public class NavDrawerListAdapter extends ArrayAdapter<NavRow> {
    private final Context mContext;
    private final ArrayList<NavRow> mValues;

    public NavDrawerListAdapter(Context context, ArrayList<NavRow> values) {
        super(context, R.layout.nav_drawer_row, values);
        this.mContext = context;
        this.mValues = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.nav_drawer_row, parent, false);
        TextView menuTextView = (TextView) rowView.findViewById(R.id.menu_text);
        ImageView iconView = (ImageView) rowView.findViewById(R.id.icon);
        NavRow currentRow = mValues.get(position);
        menuTextView.setText(currentRow.getRowText());
        iconView.setImageResource(currentRow.getIconResource());
//        Drawable icon = getContext().getResources().get
//        iconView.setImageDrawable();

        return rowView;
    }
}
