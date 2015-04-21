package fi.bitrite.android.ws.activity;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.HostBriefInfo;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        ImageView icon = (ImageView) hostListItem.findViewById(R.id.icon);
        TextView memberInfo = (TextView) hostListItem.findViewById(R.id.txtMemberInfo);

        String hostFullname = host.getFullname();
        if (mQuery != null) {
            if (host.getCity().contains(mQuery)) {
                location.setTextColor(0xffffff);
                location.setTypeface(null, Typeface.BOLD);
            } else {
                // fullname.setTypeface(null, Typeface.BOLD);
            }
        }
        if (host.getNotCurrentlyAvailable()) {
            icon.setImageResource(R.drawable.ic_home_variant_grey600_24dp);
            icon.setAlpha(0.5f);
            hostFullname += " " + mContext.getString(R.string.host_not_currently_available);
        }

        DateFormat simpleDate = DateFormat.getDateInstance();
        String activeDate = simpleDate.format(host.getAccessAsDate());
        String createdDate = new SimpleDateFormat("yyyy").format(host.getCreatedAsDate());

        String memberString = mContext.getString(R.string.search_host_summary, createdDate, activeDate);
        memberInfo.setText(memberString);


        fullname.setText(hostFullname);
        location.setText(host.getLocation());

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
