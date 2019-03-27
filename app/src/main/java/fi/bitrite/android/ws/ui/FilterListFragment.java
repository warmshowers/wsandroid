package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.xw.repo.BubbleSeekBar;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.ui.util.UserFilterManager;

public class FilterListFragment extends BaseFragment implements
        BubbleSeekBar.OnProgressChangedListener {

    @Inject UserFilterManager mUserFilterManager;
    @BindView(R.id.filter_seek_last_access) BubbleSeekBar mSeekLastAccess;
    @BindView(R.id.filter_lbl_last_access) TextView mLblLastAccess;
    @BindView(R.id.filter_ckb_currently_available) CheckBox mCkbCurrentlyAvailable;
    @BindView(R.id.filter_ckb_favorite_user) CheckBox mCkbFavoriteUser;

    private final List<FilterEntry> mFilterEntries = new ArrayList<>();

    public static Fragment create() {
        Bundle bundle = new Bundle();

        Fragment fragment = new FilterListFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilterEntries.add(new FilterEntry(
                1,
                getString(R.string.filter_diff_today),
                getString(R.string.filter_diff_short_today)));
        mFilterEntries.add(new FilterEntry(
                7,
                getString(R.string.filter_diff_this_week),
                getString(R.string.filter_diff_short_this_week)));
        mFilterEntries.add(new FilterEntry(
                30,
                getString(R.string.filter_diff_this_month),
                getString(R.string.filter_diff_short_this_month)));
        mFilterEntries.add(new FilterEntry(
                365,
                getString(R.string.filter_diff_this_year),
                getString(R.string.filter_diff_short_this_year)));
        mFilterEntries.add(new FilterEntry(
                -1,
                getString(R.string.filter_diff_any),
                getString(R.string.filter_diff_short_any)));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter_list, container, false);
        ButterKnife.bind(this, view);

        mSeekLastAccess.getConfigBuilder()
                .min(0)
                .max(mFilterEntries.size()- 1)
                .sectionCount(mFilterEntries.size()- 1)
                .build();
        mSeekLastAccess.setCustomSectionTextArray((sectionCount, array) -> {
            array.clear();
            for (FilterEntry filterEntry : mFilterEntries) {
                array.put(array.size(), filterEntry.titleShort);
            }
            return array;
        });
        mSeekLastAccess.setOnProgressChangedListener(this);
        mSeekLastAccess.setProgress(convertLastAccessDaysToProgress(
                mUserFilterManager.getFilterValue(UserFilterManager.RECENT_ACTIVITY_FILTER_KEY)));

        mCkbCurrentlyAvailable.setChecked(
            mUserFilterManager.isFilterActive(UserFilterManager.CURRENTLY_AVAILABLE_FILTER_KEY));

        mCkbFavoriteUser.setChecked(
            mUserFilterManager.isFilterActive(UserFilterManager.FAVORITE_USER_FILTER_KEY));
        return view;
    }

    @OnClick(R.id.filter_ckb_currently_available)
    void onCurrentlyAvailableClicked(View view) {
        mUserFilterManager.setFilterActivated(
                UserFilterManager.CURRENTLY_AVAILABLE_FILTER_KEY,
                mCkbCurrentlyAvailable.isChecked());
    }

    @OnClick(R.id.filter_ckb_favorite_user)
    void onFavoriteHostClicked(View view) {
        mUserFilterManager.setFilterActivated(
                UserFilterManager.FAVORITE_USER_FILTER_KEY,
                mCkbFavoriteUser.isChecked());
    }

    @OnClick(R.id.filter_btn_clear)
    void onClearFiltersClicked(View view) {
        mSeekLastAccess.setProgress(mSeekLastAccess.getMax());
        mUserFilterManager.updateFilterValue(UserFilterManager.RECENT_ACTIVITY_FILTER_KEY, -1);
        mUserFilterManager.deactivateFilter(UserFilterManager.RECENT_ACTIVITY_FILTER_KEY);

        mCkbCurrentlyAvailable.setChecked(false);
        mUserFilterManager.deactivateFilter(UserFilterManager.CURRENTLY_AVAILABLE_FILTER_KEY);

        mCkbFavoriteUser.setChecked(false);
        mUserFilterManager.deactivateFilter(UserFilterManager.FAVORITE_USER_FILTER_KEY);
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_filter_list);
    }

    @Override
    public void onProgressChanged(BubbleSeekBar seekBar, int progress, float progressFloat,
                                  boolean fromUser) {
        FilterEntry filterEntry = mFilterEntries.get(progress);
        mLblLastAccess.setText(filterEntry.title);
        mUserFilterManager.updateFilterValue(
                UserFilterManager.RECENT_ACTIVITY_FILTER_KEY,
                filterEntry.days);
        if (fromUser) {
            mUserFilterManager.activateFilter(UserFilterManager.RECENT_ACTIVITY_FILTER_KEY);
        }
    }
    @Override
    public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress,
                                      float progressFloat) { }
    @Override
    public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat,
                                     boolean fromUser) { }

    private int convertLastAccessDaysToProgress(int days) {
        for (int i = 0; i < mFilterEntries.size(); ++i) {
            if (days == mFilterEntries.get(i).days) {
                return i;
            }
        }
        return mFilterEntries.size() - 1;
    }

    private class FilterEntry {
        final int days;
        final String title;
        final String titleShort;

        FilterEntry(int days, String title, String titleShort) {
            this.days = days;
            this.title = title;
            this.titleShort = titleShort;
        }
    }
}
