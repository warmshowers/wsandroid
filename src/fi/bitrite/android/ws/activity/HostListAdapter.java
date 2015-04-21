package fi.bitrite.android.ws.activity;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.HostBriefInfo;

import java.lang.reflect.Type;
import java.util.List;

public class HostListAdapter extends ArrayAdapter<HostBriefInfo> {

    private int mResource;
    private Context mContext;
    private String mQuery;

    public HostListAdapter(Context context, int resource, String query, List<HostBriefInfo> hosts) {
        super(context, resource, hosts);
        mContext = context;
        mQuery = query;
        mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout hostListItem = inflateView(convertView);

        HostBriefInfo host = getItem(position);

        TextView fullname = (TextView) hostListItem.findViewById(R.id.txtHostFullname);
        TextView location = (TextView) hostListItem.findViewById(R.id.txtHostLocation);

        if (mQuery != null) {
            if (host.getCity().contains(mQuery)) {
                location.setTypeface(null, Typeface.BOLD);
            } else {
                // fullname.setTypeface(null, Typeface.BOLD);
            }
        }


        fullname.setText(host.getFullname());
        location.setText(host.getLocation());

        String availability = host.getNotCurrentlyAvailable() ? mContext.getString(R.string.host_not_currently_available) : mContext.getString(R.string.host_currently_available);

        return hostListItem;
    }

    private LinearLayout inflateView(View convertView) {
        LinearLayout view;

        if (convertView == null) {
            view = new LinearLayout(getContext());
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
            vi.inflate(mResource, view, true);
        } else {
            view = (LinearLayout) convertView;
        }

        return view;
    }
}
