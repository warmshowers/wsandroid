package fi.bitrite.android.ws.ui;

import android.support.v4.app.Fragment;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.ui.util.ActionBarTitleHelper;

public abstract class BaseFragment extends Fragment implements Injectable {

    @Inject ActionBarTitleHelper mActionBarTitleHelper;

    @Override
    public void onResume() {
        super.onResume();

        setTitle(getTitle());
    }

    protected abstract CharSequence getTitle();
    protected void setTitle(CharSequence title) {
        mActionBarTitleHelper.set(title);
    }
}
