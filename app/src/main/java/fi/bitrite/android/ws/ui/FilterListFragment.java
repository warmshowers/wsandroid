package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.ui.util.UserFilterManager;

public class FilterListFragment extends BaseFragment implements SeekBar.OnSeekBarChangeListener {

    @Inject UserFilterManager mUserFilterManager;
    @BindView(R.id.filter_seek_last_access) SeekBar mSeekLastAccess;
    @BindView(R.id.filter_lbl_last_access) TextView mLblLastAccess;
    @BindView(R.id.filter_ckb_currently_available) CheckBox mCkbCurrentlyAvailable;
    @BindView(R.id.filter_ckb_favorite_user) CheckBox mCkbFavoriteUser;

    public static Fragment create() {
        Bundle bundle = new Bundle();

        Fragment fragment = new FilterListFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter_list, container, false);
        ButterKnife.bind(this, view);
        mSeekLastAccess.setOnSeekBarChangeListener(this);
        mSeekLastAccess.setProgress(convertLastAccessDaysToProgress(
                mUserFilterManager.getFilterValue(UserFilterManager.RECENT_ACTIVITY_FILTER_KEY)));
        mSeekLastAccess.setMax(mProgressToDays.length-1);

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
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      mLblLastAccess.setText(convertLastAccessProgressToText(progress));
      mUserFilterManager.updateFilterValue(UserFilterManager.RECENT_ACTIVITY_FILTER_KEY,
          convertLastAccessProgressToDays(progress));
      if (fromUser) {
          mUserFilterManager.activateFilter(UserFilterManager.RECENT_ACTIVITY_FILTER_KEY);
      }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    private final static Integer[] mProgressToDays = {1, 7, 30, -1};
    private static int convertLastAccessProgressToDays(int progress) {
        return mProgressToDays[progress];
    }

    // Inversion of convertLastAccessProgressToDays.
    private static int convertLastAccessDaysToProgress(int days) {
        for (int i = 0; i < mProgressToDays.length; ++i) {
            if (days == mProgressToDays[i]) {
                return i;
            }
        }
        return mProgressToDays.length - 1;
    }

    private String convertLastAccessProgressToText(int progress) {
        final int days = convertLastAccessProgressToDays(progress);
        switch (days) {
            case 1: return getString(R.string.filter_diff_today);
            case 7: return getString(R.string.filter_diff_this_week);
            case 30: return getString(R.string.filter_diff_this_month);
            default: return getString(R.string.filter_diff_any);
        }
    }
}
