package fi.bitrite.android.ws.activity;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;

public class HostListAdapter extends ArrayAdapter<Host> {

	private int resource;
	
	public HostListAdapter(Context context, int resource, List<Host> hosts) {
		super(context, resource, hosts);
		this.resource = resource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout hostListItem = inflateView(convertView);

		Host host = getItem(position);
		
		TextView fullname = (TextView) hostListItem.findViewById(R.id.txtHostFullname);
		TextView comments = (TextView) hostListItem.findViewById(R.id.txtHostComments);
		
		fullname.setText(host.getFullname());
		comments.setText(host.getComments());
		
		return hostListItem;
	}		

	private LinearLayout inflateView(View convertView) {
		LinearLayout view;
		
		if(convertView == null) {
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
