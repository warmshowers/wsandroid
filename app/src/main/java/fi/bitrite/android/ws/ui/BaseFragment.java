package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.ui.util.ActionBarTitleHelper;
import fi.bitrite.android.ws.ui.util.NavigationController;
import io.reactivex.disposables.CompositeDisposable;

public abstract class BaseFragment extends Fragment implements Injectable {

    @Inject ActionBarTitleHelper mActionBarTitleHelper;

    private CompositeDisposable mCreateDestroyDisposable = new CompositeDisposable();
    private CompositeDisposable mResumePauseDisposable = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCreateDestroyDisposable = new CompositeDisposable();
    }
    @Override
    public void onResume() {
        super.onResume();

        mResumePauseDisposable = new CompositeDisposable();
        setTitle(getTitle());
    }
    @Override
    public void onPause() {
        mResumePauseDisposable.dispose();
        super.onPause();
    }
    @Override
    public void onDestroy() {
        mCreateDestroyDisposable.dispose();
        super.onDestroy();
    }

    protected abstract CharSequence getTitle();
    protected void setTitle(CharSequence title) {
        mActionBarTitleHelper.set(title);
    }

    NavigationController getNavigationController() {
        return ((MainActivity)getActivity()).getNavigationController();
    }

    CompositeDisposable getCreateDestroyDisposable() {
        return mCreateDestroyDisposable;
    }
    CompositeDisposable getResumePauseDisposable() {
        return mResumePauseDisposable;
    }
}
