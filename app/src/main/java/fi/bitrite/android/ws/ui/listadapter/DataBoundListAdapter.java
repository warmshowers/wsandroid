package fi.bitrite.android.ws.ui.listadapter;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * A generic RecyclerView adapter that uses Data Binding & DiffUtil.
 *
 * @param <T> Type of the items in the list
 * @param <V> Type of the Binding
 */
public abstract class DataBoundListAdapter<T, V extends DataBoundListAdapter.ViewDataBinding<T>>
        extends RecyclerView.Adapter<DataBoundListAdapter<T, V>.ViewHolder> {

    public interface ViewDataBinding<T> {
        View getRoot();

        void bind(T item);
    }

    /**
     * A generic ViewHolder that works with a {@link ViewDataBinding<T>}.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        final V binding;

        ViewHolder(V binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }


    @Nullable
    private List<T> mItems;

    private int mReservedItemCount = -1;

    // Each time data is set, we update this variable so that if DiffUtil calculation returns
    // after repetitive updates, we can ignore the old calculation.
    private AtomicInteger mDataVersion = new AtomicInteger(0);

    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        V binding = createBinding(parent);
        return new ViewHolder(binding);
    }

    protected abstract V createBinding(ViewGroup parent);

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (mItems != null && position < mItems.size()) {
            holder.binding.bind(mItems.get(position));
        }
    }

    @SuppressLint("StaticFieldLeak")
    @MainThread
    public void replace(List<T> update) {
        replaceRx(update)
                .onErrorComplete() // Ignore errors
                .subscribe();
    }

    /**
     * `update` needs to be a modifiable list.
     */
    @MainThread
    public Completable replaceRx(List<T> update) {
        if (update == null || update.isEmpty()) {
            int oldSize = mItems != null ? mItems.size() : 0;
            mItems = update;
            notifyItemRangeRemoved(0, oldSize);
            return Completable.complete();
        }

        final List<T> oldItems = mItems;
        final int dataVersion = mDataVersion.incrementAndGet();
        class DiffResultWrapper {
            private DiffUtil.DiffResult result;
        }
        final DiffResultWrapper diffResultWrapper = new DiffResultWrapper();
        return Completable.create(emitter -> {
            sort(update);

            if (oldItems != null) {
                diffResultWrapper.result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return oldItems.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return update.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        T oldItem = oldItems.get(oldItemPosition);
                        T newItem = update.get(newItemPosition);
                        return DataBoundListAdapter.this.areItemsTheSame(oldItem, newItem);
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        T oldItem = oldItems.get(oldItemPosition);
                        T newItem = update.get(newItemPosition);
                        return DataBoundListAdapter.this.areContentsTheSame(oldItem, newItem);
                    }
                });
            }
            emitter.onComplete();
        })
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnComplete(() -> {
            if (dataVersion != mDataVersion.get()) {
                // ignore update
                return;
            }

            mReservedItemCount = -1;
            if (mItems == null) {
                mItems = update;
                notifyDataSetChanged();
            } else {
                boolean diffIsValid = mItems == oldItems;
                mItems = update;
                if (diffIsValid) {
                    diffResultWrapper.result.dispatchUpdatesTo(this);
                } else {
                    notifyDataSetChanged();
                    // The items changed between us calculating the diff and now.
                }
            }
        });
    }


    protected abstract void sort(List<T> data);

    /**
     * Should return true when two items represent the same element (identified by the same id).
     */
    protected abstract boolean areItemsTheSame(T oldItem, T newItem);

    /**
     * Should return true if two items do not differ in the sense that an update to the UI is
     * necessary.
     */
    protected abstract boolean areContentsTheSame(T oldItem, T newItem);

    public void reserveItems(int count) {
        mReservedItemCount = count;
    }

    @Override
    public int getItemCount() {
        return mReservedItemCount >= 0
                ? mReservedItemCount
                : mItems == null ? 0 : mItems.size();
    }
}
