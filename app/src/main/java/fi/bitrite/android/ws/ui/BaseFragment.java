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
import fi.bitrite.android.ws.util.SerialCompositeDisposable;
import io.reactivex.disposables.CompositeDisposable;

public abstract class BaseFragment extends Fragment implements Injectable {

    @Inject ActionBarTitleHelper mActionBarTitleHelper;

    private final SerialCompositeDisposable mCreateDestroyDisposable =
            new SerialCompositeDisposable();
    private final SerialCompositeDisposable mCreateDestroyViewDisposable =
            new SerialCompositeDisposable();
    private final SerialCompositeDisposable mStartStopDisposable = new SerialCompositeDisposable();
    private final SerialCompositeDisposable mResumePauseDisposable =
            new SerialCompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mCreateDestroyDisposable.reset();
        super.onCreate(savedInstanceState);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mCreateDestroyViewDisposable.reset();
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    @Override
    public void onStart() {
        mStartStopDisposable.reset();
        super.onStart();
    }
    @Override
    public void onResume() {
        mResumePauseDisposable.reset();
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
        return ((MainActivity) getActivity()).getNavigationController();
    }

    CompositeDisposable getCreateDestroyDisposable() {
        return mCreateDestroyDisposable.get();
    }
    CompositeDisposable getCreateDestroyViewDisposable() {
        return mCreateDestroyViewDisposable.get();
    }
    CompositeDisposable getStartStopDisposable() {
        return mStartStopDisposable.get();
    }
    CompositeDisposable getResumePauseDisposable() {
        return mResumePauseDisposable.get();
    }
}