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

public class StarredHostsAdapter extends ArrayAdapter<Host> {

	private int resource;
	
	public StarredHostsAdapter(Context context, int resource, List<Host> starredHosts) {
		super(context, resource, starredHosts);
		this.resource = resource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout starredHostView = inflateView(convertView);

		Host starredHost = getItem(position);
		
		TextView fullnameView = (TextView) starredHostView.findViewById(R.id.txtStarredHostFullname);
		TextView commentsView = (TextView) starredHostView.findViewById(R.id.txtStarredHostComments);
		
		fullnameView.setText(starredHost.getFullname());
		commentsView.setText(starredHost.getComments());
		
		return starredHostView;
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
