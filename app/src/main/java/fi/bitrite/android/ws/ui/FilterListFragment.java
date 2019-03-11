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
import fi.bitrite.android.ws.ui.util.HostFilterManager;

public class FilterListFragment extends BaseFragment implements SeekBar.OnSeekBarChangeListener {

    @Inject HostFilterManager mHostFilterManager;
    @BindView(R.id.last_access_seekbar) SeekBar mLastAccessSeekBar;
    @BindView(R.id.last_access_text) TextView mLastAccessText;
    @BindView(R.id.currently_available_checkbox) CheckBox mCurrentlyAvailableCheckBox;
    @BindView(R.id.favorite_host_checkbox) CheckBox mFavoriteHostCheckbox;

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
        mHostFilterManager.activateFilter(HostFilterManager.RECENT_ACTIVITY_FILTER_KEY);
        mLastAccessSeekBar.setOnSeekBarChangeListener(this);
        mLastAccessSeekBar.setProgress(convertLastAccessDaysToProgress(
              mHostFilterManager.getFilterValue(HostFilterManager.RECENT_ACTIVITY_FILTER_KEY)));
        mLastAccessSeekBar.setMax(10);

        mCurrentlyAvailableCheckBox.setChecked(
            mHostFilterManager.isFilterActive(HostFilterManager.CURRENTLY_AVAILABLE_FILTER_KEY));

        mFavoriteHostCheckbox.setChecked(
            mHostFilterManager.isFilterActive(HostFilterManager.FAVORITE_HOST_FILTER_KEY));
        return view;
    }

    @OnClick(R.id.currently_available_checkbox)
    void onCurrentlyAvailableClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        if (checked) {
            mHostFilterManager.activateFilter(HostFilterManager.CURRENTLY_AVAILABLE_FILTER_KEY);
        } else {
            mHostFilterManager.deactivateFilter(HostFilterManager.CURRENTLY_AVAILABLE_FILTER_KEY);
        }
    }

    @OnClick(R.id.favorite_host_checkbox)
    void onFavoriteHostClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        if (checked) {
            mHostFilterManager.activateFilter(HostFilterManager.FAVORITE_HOST_FILTER_KEY);
        } else {
            mHostFilterManager.deactivateFilter(HostFilterManager.FAVORITE_HOST_FILTER_KEY);
        }
    }

    @OnClick(R.id.btn_clear_filters)
    void onClearFiltersClicked(View view) {
        mLastAccessSeekBar.setProgress(10);
        mCurrentlyAvailableCheckBox.setChecked(false);
        mHostFilterManager.deactivateFilter(HostFilterManager.CURRENTLY_AVAILABLE_FILTER_KEY);
        mFavoriteHostCheckbox.setChecked(false);
        mHostFilterManager.deactivateFilter(HostFilterManager.FAVORITE_HOST_FILTER_KEY);
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_filter_list);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      mLastAccessText.setText(convertLastAccessProgressToText(progress));
      mHostFilterManager.updateFilterValue(HostFilterManager.RECENT_ACTIVITY_FILTER_KEY,
          convertLastAccessProgressToDays(progress));
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    // [0, 1, ... 10] -> {1, 2, 3, 7, 14, 30, 60, 90, 180, 365, -1}.
    private int convertLastAccessProgressToDays(int progress) {
        switch (progress) {
            case 0: return 1;
            case 1: return 2;
            case 2: return 3;
            case 3: return 7;
            case 4: return 14;
            case 5: return 30;
            case 6: return 60;
            case 7: return 90;
            case 8: return 180;
            case 9: return 365;
            case 10: return -1;
        }
        return -1;
    }

    // Inversion of convertLastAccessProgressToDays.
    private int convertLastAccessDaysToProgress(int days) {
        switch (days) {
            case 1: return 0;
            case 2: return 1;
            case 3: return 2;
            case 7: return 3;
            case 14: return 4;
            case 30: return 5;
            case 60: return 6;
            case 90: return 7;
            case 180: return 8;
            case 365: return 9;
            case -1: return 10;
        }
        return 10;
    }

    // TODO(dproctor): This needs to be internationalized. Perhaps something in
    // android.text.format.DateUtils can be used?
    private String convertLastAccessProgressToText(int progress) {
        int days = convertLastAccessProgressToDays(progress);
        if (days == 1) {
            return "1 day";
        }
        if (days >= 0 && days < 7) {
            return days + " days";
        }
        if (days == 7) {
            return "1 week";
        }
        if (days > 7 && days < 30) {
            return days / 7 + " weeks";
        }
        if (days == 30) {
            return "1 month";
        }
        if (days > 30 && days < 365) {
            return days / 30 + " months";
        }
        if (days == 365) {
            return "1 year";
        }
        if (days > 365) {
            return days / 365 + " year";
        }
        return "Any time";
    }
}
