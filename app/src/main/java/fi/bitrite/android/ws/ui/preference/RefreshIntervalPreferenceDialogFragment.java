package fi.bitrite.android.ws.ui.preference;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import fi.bitrite.android.ws.R;

public class RefreshIntervalPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    private static final String KEY_INTERVAL = "interval";

    @BindView(R.id.preference_dialog_refresh_interval_swt_enabled) Switch mSwtEnable;
    @BindView(R.id.preference_dialog_refresh_interval_edt_interval) EditText mEdtInterval;

    private int mInterval;
    private int mDefaultValue;

    public static RefreshIntervalPreferenceDialogFragment create(String key) {
        final RefreshIntervalPreferenceDialogFragment
                fragment = new RefreshIntervalPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RefreshIntervalPreference pref = getRefreshIntervalPreference();
        mDefaultValue = pref.getDefaultValue();
        if (mDefaultValue <= 0) {
            mDefaultValue = 1;
        }

        if (savedInstanceState == null) {
            mInterval = Integer.parseInt(pref.getText());
        } else {
            mInterval = savedInstanceState.getInt(KEY_INTERVAL);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_INTERVAL, mInterval);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ButterKnife.bind(this, view);

        mSwtEnable.setChecked(mInterval > 0);
        mEdtInterval.setText(Integer.toString(mInterval > 0 ? mInterval : mDefaultValue));
        mEdtInterval.setEnabled(mSwtEnable.isChecked());
    }

    private RefreshIntervalPreference getRefreshIntervalPreference() {
        return (RefreshIntervalPreference) getPreference();
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    protected boolean needInputMethod() {
        // We want the input method to show, if possible, when dialog is displayed
        return true;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            try {
                mInterval = mSwtEnable.isChecked()
                        ? Integer.parseInt(mEdtInterval.getText().toString())
                        : 0;
            } catch (NumberFormatException e) {
                mInterval = 0;
            }

            RefreshIntervalPreference pref = getRefreshIntervalPreference();
            if (pref.callChangeListener(mInterval)) {
                pref.setText(Integer.toString(mInterval));
            }
        }
    }

    @OnCheckedChanged(R.id.preference_dialog_refresh_interval_swt_enabled)
    void onEnabledChanged(boolean checked) {
        mEdtInterval.setEnabled(checked);
    }
}
