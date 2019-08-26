package fi.bitrite.android.ws.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.MessageThread;
import fi.bitrite.android.ws.repository.MessageRepository;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.listadapter.MessageThreadListAdapter;
import fi.bitrite.android.ws.ui.util.DividerItemDecoration;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class MessageThreadsFragment extends BaseFragment {

    private static final String ICICLE_KEY_THREADS_STATE = "threads_state";

    @Inject MessageRepository mMessageRepository;
    @Inject UserRepository mUserRepository;
    @Inject LoggedInUserHelper mLoggedInUserHelper;

    @BindView(R.id.threads_swipe_refresh) SwipeRefreshLayout mSwipeRefresh;
    @BindView(R.id.threads_lists) RecyclerView mThreadList;

    private boolean mDidReload = false;
    private MessageThreadListAdapter mThreadListAdapter;

    private Parcelable mThreadListState;

    public static Fragment create() {
        Bundle bundle = new Bundle();

        Fragment fragment = new MessageThreadsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_message_threads, container, false);
        ButterKnife.bind(this, view);

        // TODO(saemy): Make reloads more intelligent than just every time we load the fragment.
        // Last reloaded?
        if (!mDidReload) {
            mDidReload = true;
            reloadThreads();
        }

        mSwipeRefresh.setOnRefreshListener(this::reloadThreads);

        // Initializes the message list.
        if (mThreadListAdapter == null) {
            mThreadListAdapter = new MessageThreadListAdapter(
                    mLoggedInUserHelper, mMessageRepository, getNavigationController(),
                    mUserRepository);
        }
        mThreadList.setAdapter(mThreadListAdapter);

        // Add dividers between message threads in the recycler view.
        LinearLayoutManager layoutManager = (LinearLayoutManager) mThreadList.getLayoutManager();
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                mThreadList.getContext(), layoutManager.getOrientation(), false);
        mThreadList.addItemDecoration(dividerItemDecoration);

        if (savedInstanceState != null) {
            mThreadListState = savedInstanceState.getParcelable(ICICLE_KEY_THREADS_STATE);
        }

        init();

        return view;
    }

    @SuppressLint("UseSparseArrays")
    public void init() {
        class Container {
            private Map<Integer, MessageThread> threads;
            private Disposable disposable;
        }
        final Container c = new Container();
        getCreateDestroyViewDisposable().add(mMessageRepository
                .getAll()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(threadObservables -> {
                    if (c.disposable != null) {
                        c.disposable.dispose();
                    }

                    // Reserves the number of items that eventually are going to be populated in the
                    // list. Then it scrolls to the previously preserved position (after recreation
                    // of the fragment).
                    mThreadListAdapter.reserveItems(threadObservables.size());
                    if (mThreadListState != null) {
                        LinearLayoutManager layoutManager =
                                (LinearLayoutManager) mThreadList.getLayoutManager();
                        layoutManager.onRestoreInstanceState(mThreadListState);
                        mThreadListState = null;
                    }

                    c.threads = new HashMap<>(threadObservables.size()); // No sparse. We need #values().
                    c.disposable = Observable.merge(threadObservables)
                            .filter(Resource::hasData)
                            .map(threadResource -> threadResource.data)
                            .map(thread -> {
                                c.threads.put(thread.id, thread);
                                return thread;
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            // Buffers updates together to avoid flickering effect.
                            .debounce(100, TimeUnit.MILLISECONDS)
                            .subscribe(thread ->
                                    mThreadListAdapter.replace(new ArrayList<>(c.threads.values())));
                    getCreateDestroyViewDisposable().add(c.disposable);
                }));
    }

    private void reloadThreads() {
        mSwipeRefresh.setRefreshing(true);
        getCreateDestroyViewDisposable().add(
                mMessageRepository.reloadThreads()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnEvent(t -> mSwipeRefresh.setRefreshing(false))
                        .subscribe(() -> {}, throwable -> Toast.makeText(
                                getContext(), R.string.messages_reload_failed, Toast.LENGTH_LONG)
                                .show()));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Saves the threadList's offset.
        LinearLayoutManager layoutManager = (LinearLayoutManager) mThreadList.getLayoutManager();
        Parcelable threadListState = layoutManager.onSaveInstanceState();
        outState.putParcelable(ICICLE_KEY_THREADS_STATE, threadListState);
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.navigation_item_messages);
    }
}
