package fi.bitrite.android.ws.ui.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

import fi.bitrite.android.ws.R;

public class RefreshIntervalPreference extends EditTextPreference {
    private int mDefaultValue;

    public RefreshIntervalPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_dialog_refresh_interval);
    }
    public RefreshIntervalPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setDialogLayoutResource(R.layout.preference_dialog_refresh_interval);
    }
    public RefreshIntervalPreference(Context context, AttributeSet attrs, int defStyleAttr,
                                     int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setDialogLayoutResource(R.layout.preference_dialog_refresh_interval);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        Object defaultObj = super.onGetDefaultValue(a, index);
        if (defaultObj != null && defaultObj instanceof String) {
            try {
                mDefaultValue = Integer.parseInt((String) defaultObj);
            } catch (NumberFormatException e) {
            }
        }
        return defaultObj;
    }

    public int getDefaultValue() {
        return mDefaultValue;
    }
}
