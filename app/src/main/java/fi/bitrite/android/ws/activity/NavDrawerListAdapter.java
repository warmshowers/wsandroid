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
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.model.NavRow;

/**
 * Created by rfay on 4/19/15.
 */
public class NavDrawerListAdapter extends ArrayAdapter<NavRow> {
    private final Context mContext;
    private final ArrayList<NavRow> mValues;
    private final int mCurrentActivityIndex;

    public NavDrawerListAdapter(Context context, ArrayList<NavRow> values, int currentActivity) {
        super(context, R.layout.nav_drawer_row, values);
        mContext = context;
        mValues = values;
        mCurrentActivityIndex = currentActivity;
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
        if (position == mCurrentActivityIndex) {
            rowView.setBackgroundColor(WSAndroidApplication.getAppContext().getResources().getColor(R.color.backgroundLightGrey));
        }
        return rowView;
    }
}
