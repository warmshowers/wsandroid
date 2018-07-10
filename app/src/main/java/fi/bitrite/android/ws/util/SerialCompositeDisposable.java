package fi.bitrite.android.ws.util;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.SerialDisposable;
import io.reactivex.internal.disposables.DisposableContainer;

/**
 * Combination of {@link SerialDisposable} and {@link CompositeDisposable}.
 */
public final class SerialCompositeDisposable implements Disposable, DisposableContainer {
    private final SerialDisposable mDisposable = new SerialDisposable();
    private CompositeDisposable mCompositeDisposable;

    public SerialCompositeDisposable() {
        reset();
    }

    /**
     * Does not dispose this disposable but the containing one.
     */
    @Override
    public void dispose() {
        mCompositeDisposable.dispose();
    }

    @Override
    public boolean isDisposed() {
        return mCompositeDisposable.isDisposed();
    }

    public void reset() {
        mCompositeDisposable = new CompositeDisposable();
        mDisposable.set(mCompositeDisposable);
    }

    public CompositeDisposable get() {
        return mCompositeDisposable;
    }

    @Override
    public boolean add(Disposable d) {
        return mCompositeDisposable.add(d);
    }
    @Override
    public boolean remove(Disposable d) {
        return mCompositeDisposable.remove(d);
    }
    @Override
    public boolean delete(Disposable d) {
        return mCompositeDisposable.delete(d);
    }
}
