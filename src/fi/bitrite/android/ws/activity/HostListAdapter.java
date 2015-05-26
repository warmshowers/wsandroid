package fi.bitrite.android.ws.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.HostBriefInfo;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HostListAdapter extends ArrayAdapter<HostBriefInfo> {

    private int mResource;
    private Context mContext;
    private String mQuery;

    public HostListAdapter(Context context, int resource, String query, List<HostBriefInfo> hosts) {
        super(context, resource, hosts);
        mContext = context;
        mQuery = (query != null && !query.isEmpty()) ? query.toLowerCase() : null;
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

        // Emphasize the name or location if it's a match on the search
        if (mQuery != null) {

            final String cityString = host.getLocation().toLowerCase();


            //TODO: Special caracters match in search but not in display
            if (Pattern.compile(Pattern.quote(mQuery), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(cityString).find() != false) {
                location.setText(getTextMatch(mQuery, cityString));
            } else {
                location.setText(host.getLocation());
            }

            //Toast.makeText(mContext, "HostListAdp hostFullname = " + hostFullname + " - " + mQuery + " - " + Pattern.compile(Pattern.quote(mQuery), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(hostFullname).find(), Toast.LENGTH_SHORT).show();

            if (Pattern.compile(Pattern.quote(mQuery), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(hostFullname).find() != false) {
                fullname.setText(getTextMatch(mQuery,hostFullname));
            } else {
                fullname.setText(hostFullname);
            }
        } else {
            fullname.setText(hostFullname);
            location.setText(host.getLocation());
        }

        // Divider
        if (position == 0) {
            hostListItem.findViewById(R.id.divider).setVisibility(View.GONE);
        } else {
            hostListItem.findViewById(R.id.divider).setVisibility(View.VISIBLE);
        }

        // Set the host icon to black if they're available, otherwise gray
        if (host.getNotCurrentlyAvailable()) {
            icon.setImageResource(R.drawable.ic_home_variant_grey600_24dp);
            icon.setAlpha(0.5f);
            hostFullname += " " + mContext.getString(R.string.host_not_currently_available);
        } else {
            icon.setImageResource(R.drawable.ic_home_variant_black_24dp);
            icon.setAlpha(1.0f);
        }

        DateFormat simpleDate = DateFormat.getDateInstance();
        String activeDate = simpleDate.format(host.getLastLoginAsDate());
        String createdDate = new SimpleDateFormat("yyyy").format(host.getCreatedAsDate());

        String memberString = mContext.getString(R.string.search_host_summary, createdDate, activeDate);
        memberInfo.setText(memberString);

        return hostListItem;
    }

    private SpannableStringBuilder getTextMatch(String mPattern, String mMatch) {

        final Pattern p = Pattern.compile(mPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        final Matcher matcher = p.matcher(mMatch);

        // TODO: ignore accents and other special characters
        final SpannableStringBuilder spannable = new SpannableStringBuilder(mMatch);
        final ForegroundColorSpan span = new ForegroundColorSpan(mContext.getResources().getColor(R.color.primaryColorAccent));
        while (matcher.find()) {
            spannable.setSpan(
                    span, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        return spannable;
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
