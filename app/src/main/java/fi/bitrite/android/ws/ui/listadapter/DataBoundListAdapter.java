package fi.bitrite.android.ws.ui.listadapter;

import android.annotation.SuppressLint;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
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
    private List<T> items;

    private int mReservedItemCount = -1;

    // Each time data is set, we update this variable so that if DiffUtil calculation returns
    // after repetitive updates, we can ignore the old calculation.
    private int dataVersion = 0;

    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        V binding = createBinding(parent);
        return new ViewHolder(binding);
    }

    protected abstract V createBinding(ViewGroup parent);

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (items != null && position < items.size()) {
            holder.binding.bind(items.get(position));
        }
    }

    @SuppressLint("StaticFieldLeak")
    @MainThread
    public void replace(List<T> update) {
        replaceRx(update)
                .onErrorComplete() // Ignore errors
                .subscribe();
    }

    @MainThread
    public Completable replaceRx(List<T> update) {
        return Completable.create(emitter -> {
            dataVersion++;

            mReservedItemCount = -1;

            if (update != null) {
                sort(update);
            }

            if (items == null) {
                if (update != null) {
                    items = update;
                    notifyDataSetChanged();
                }
                emitter.onComplete();
            } else if (update == null) {
                int oldSize = items.size();
                items = null;
                notifyItemRangeRemoved(0, oldSize);
                emitter.onComplete();
            } else {
                final int startVersion = dataVersion;
                final List<T> oldItems = items;

                Single.<DiffUtil.DiffResult>create(emitter2 -> emitter2.onSuccess(
                        DiffUtil.calculateDiff(new DiffUtil.Callback() {
                            @Override
                            public int getOldListSize() {
                                return oldItems.size();
                            }

                            @Override
                            public int getNewListSize() {
                                return update.size();
                            }

                            @Override
                            public boolean areItemsTheSame(int oldItemPosition,
                                                           int newItemPosition) {
                                T oldItem = oldItems.get(oldItemPosition);
                                T newItem = update.get(newItemPosition);
                                return DataBoundListAdapter.this.areItemsTheSame(oldItem, newItem);
                            }

                            @Override
                            public boolean areContentsTheSame(int oldItemPosition,
                                                              int newItemPosition) {
                                T oldItem = oldItems.get(oldItemPosition);
                                T newItem = update.get(newItemPosition);
                                return DataBoundListAdapter.this.areContentsTheSame(oldItem,
                                        newItem);
                            }
                        })))
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(diffResult -> {
                            if (startVersion != dataVersion) {
                                // ignore update
                                return 0;
                            }
                            items = update;
                            diffResult.dispatchUpdatesTo(DataBoundListAdapter.this);
                            return 0;
                        })
                        .toCompletable()
                        .subscribe(emitter::onComplete);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
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
                : items == null ? 0 : items.size();
    }
}
