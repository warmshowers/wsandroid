package fi.bitrite.android.ws.activity;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.Tools;

import java.util.List;

public class HostListAdapter extends ArrayAdapter<HostBriefInfo> {

    private int[] colors = new int[] { 0xFF000000, 0xFF222222 };
    
    private int resource;

    public HostListAdapter(Context context, int resource, List<HostBriefInfo> hosts) {
        super(context, resource, hosts);
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout hostListItem = inflateView(convertView);

        int colorPos = position % colors.length;
        hostListItem.setBackgroundColor(colors[colorPos]);
        
        HostBriefInfo host = getItem(position);

        TextView fullname = (TextView) hostListItem.findViewById(R.id.txtHostFullname);
        TextView location = (TextView) hostListItem.findViewById(R.id.txtHostLocation);
        TextView comments = (TextView) hostListItem.findViewById(R.id.txtHostComments);
        TextView updated =  (TextView) hostListItem.findViewById(R.id.txtHostUpdated);

        fullname.setText(host.getFullname());
        location.setText(host.getLocation());
        
        // Allow such TextView html as it will; but Drupal's text assumes linefeeds break lines
        comments.setText(Tools.siteHtmlToHtml(host.getComments()));
        
        if (host.getUpdated() != null) {
            updated.setText(getContext().getResources().getString(R.string.last_updated) + " " + host.getUpdated());
            updated.setVisibility(View.VISIBLE);
        } else {
            updated.setVisibility(View.GONE);
            
        }

        return hostListItem;
    }

    private LinearLayout inflateView(View convertView) {
        LinearLayout view;

        if (convertView == null) {
            view = new LinearLayout(getContext());
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
            vi.inflate(resource, view, true);
        } else {
            view = (LinearLayout) convertView;
        }

        return view;
    }
}
