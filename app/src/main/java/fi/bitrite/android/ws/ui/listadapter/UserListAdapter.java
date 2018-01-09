package fi.bitrite.android.ws.ui.listadapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.Host;

public class UserListAdapter extends ArrayAdapter<Host> {

    private final String mQuery;
    private final Pattern mQueryPattern;

    @BindView(R.id.user_list_icon) ImageView mIcon;
    @BindView(R.id.user_list_lbl_fullname) TextView mLblFullname;
    @BindView(R.id.user_list_lbl_location) TextView mLblLocation;
    @BindView(R.id.user_list_lbl_member_info) TextView mMemberInfo;
    @BindView(R.id.user_list_divider) View mDivider;
    @BindColor(R.color.primaryColorAccent) int mForegroundColor;

    public UserListAdapter(Context context, String query, List<Host> users) {
        super(context, R.layout.item_user_list, users);

        mQuery = TextUtils.isEmpty(query) ? null : query.toLowerCase();

        mQueryPattern = mQuery != null
                ? Pattern.compile(Pattern.quote(mQuery), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                : null;
    }

    public void resetDataset(List<Host> users) {
        clear();
        addAll(users);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Host user = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_user_list, parent, false);
        }

        ButterKnife.bind(this, convertView);

        String hostFullname = user.getFullname();

        // Emphasize the name or location if it's a match on the search
        if (mQuery != null) {
            final String cityString = user.getLocation().toLowerCase();

            //TODO: Special caracters match in search but not in display
            mLblLocation.setText(mQueryPattern.matcher(cityString).find()
                    ? getTextMatch(mQuery, cityString)
                    : user.getLocation());

            //Toast.makeText(mContext, "HostListAdp hostFullname = " + hostFullname + " - " + mQuery + " - " + Pattern.compile(Pattern.quote(mQuery), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(hostFullname).find(), Toast.LENGTH_SHORT).show();

            mLblFullname.setText(mQueryPattern.matcher(hostFullname).find()
                    ? getTextMatch(mQuery,hostFullname)
                    : hostFullname);
        } else {
            mLblFullname.setText(hostFullname);
            mLblLocation.setText(user.getLocation());
        }

        // Divider
        // TODO(saemy): Automatic divider?
        mDivider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);

        // Set the host icon to black if they're available, otherwise gray
        mIcon.setImageResource(user.isNotCurrentlyAvailable()
                ? R.drawable.ic_home_variant_grey600_24dp
                : R.drawable.ic_home_variant_black_24dp);
        mIcon.setAlpha(user.isNotCurrentlyAvailable() ? 0.5f : 1.0f);

        DateFormat simpleDate = DateFormat.getDateInstance();
        String activeDate = simpleDate.format(user.getLastLoginAsDate());
        String createdDate = new SimpleDateFormat("yyyy").format(user.getCreatedAsDate());

        String memberString = getContext().getString(R.string.search_host_summary, createdDate, activeDate);
        mMemberInfo.setText(memberString);

        return convertView;
    }

    private SpannableStringBuilder getTextMatch(String mPattern, String mMatch) {
        final Pattern p = Pattern.compile(mPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        final Matcher matcher = p.matcher(mMatch);

        // TODO: ignore accents and other special characters
        final SpannableStringBuilder spannable = new SpannableStringBuilder(mMatch);
        final ForegroundColorSpan span = new ForegroundColorSpan(mForegroundColor);
        while (matcher.find()) {
            spannable.setSpan(
                    span, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }
}
