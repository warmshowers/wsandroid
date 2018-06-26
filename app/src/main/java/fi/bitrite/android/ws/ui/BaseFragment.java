package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.ui.util.ActionBarTitleHelper;
import fi.bitrite.android.ws.ui.util.NavigationController;
import io.reactivex.disposables.CompositeDisposable;

public abstract class BaseFragment extends Fragment implements Injectable {

    @Inject ActionBarTitleHelper mActionBarTitleHelper;

    private CompositeDisposable mCreateDestroyDisposable = new CompositeDisposable();
    private CompositeDisposable mCreateDestroyViewDisposable = new CompositeDisposable();
    private CompositeDisposable mStartStopDisposable = new CompositeDisposable();
    private CompositeDisposable mResumePauseDisposable = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCreateDestroyDisposable = new CompositeDisposable();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mCreateDestroyViewDisposable = new CompositeDisposable();
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    @Override
    public void onStart() {
        mStartStopDisposable = new CompositeDisposable();
        super.onStart();
    }
    @Override
    public void onResume() {
        mResumePauseDisposable = new CompositeDisposable();
        super.onResume();
        setTitle(getTitle());
    }
    @Override
    public void onPause() {
        mResumePauseDisposable.dispose();
        super.onPause();
    }
    @Override
    public void onStop() {
        mStartStopDisposable.dispose();
        super.onStop();
    }
    @Override
    public void onDestroyView() {
        mCreateDestroyViewDisposable.dispose();
        super.onDestroyView();
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
    CompositeDisposable getCreateDestroyViewDisposable() {
        return mCreateDestroyViewDisposable;
    }
    CompositeDisposable getStartStopDisposable() {
        return mStartStopDisposable;
    }
    CompositeDisposable getResumePauseDisposable() {
        return mResumePauseDisposable;
    }
}
