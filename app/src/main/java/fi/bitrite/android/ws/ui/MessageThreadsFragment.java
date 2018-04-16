package fi.bitrite.android.ws.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import fi.bitrite.android.ws.ui.util.NavigationController;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MessageThreadsFragment extends BaseFragment {

    private static final String ICICLE_KEY_THREADS_STATE = "threads_state";

    @Inject MessageRepository mMessageRepository;
    @Inject UserRepository mUserRepository;
    @Inject LoggedInUserHelper mLoggedInUserHelper;
    @Inject NavigationController mNavigationController;

    @BindView(R.id.threads_swipe_refresh) SwipeRefreshLayout mSwipeRefresh;
    @BindView(R.id.threads_lists) RecyclerView mThreadList;

    private boolean mDidReload = false;
    private Disposable mDisposable;
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
        View view = inflater.inflate(R.layout.fragment_message_threads, container, false);
        ButterKnife.bind(this, view);

        // TODO(saemy): Make reloads more intelligent than just every time we load the fragment.
        if (!mDidReload) {
            mDidReload = true;
            reloadThreads();
        }

        mSwipeRefresh.setOnRefreshListener(this::reloadThreads);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle icicle) {
        super.onActivityCreated(icicle);

        mThreadListAdapter = new MessageThreadListAdapter(
                mLoggedInUserHelper, mMessageRepository, mNavigationController, mUserRepository);
        mThreadList.setAdapter(mThreadListAdapter);

        // Add dividers between message threads in the recycler view.
        LinearLayoutManager layoutManager = (LinearLayoutManager) mThreadList.getLayoutManager();
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                mThreadList.getContext(), layoutManager.getOrientation(), false);
        mThreadList.addItemDecoration(dividerItemDecoration);

        if (icicle != null) {
            mThreadListState = icicle.getParcelable(ICICLE_KEY_THREADS_STATE);
        }
    }

    private void reloadThreads() {
        mSwipeRefresh.setRefreshing(true);
        mMessageRepository.reloadThreads()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> mSwipeRefresh.setRefreshing(false),
                        throwable -> {
                            mSwipeRefresh.setRefreshing(false);

                            Toast.makeText(getContext(), R.string.messages_reload_failed,
                                    Toast.LENGTH_SHORT).show();
                        });
    }

    @SuppressLint("UseSparseArrays")
    @Override
    public void onResume() {
        super.onResume();

        class Container {
            private Map<Integer, MessageThread> threads;
            private Disposable disposable;
        }
        final Container c = new Container();
        mDisposable = mMessageRepository.getAll().subscribe(threadObservables -> {
            if (c.disposable != null) {
                c.disposable.dispose();
            }

            // Reserves the number of items that eventually are going to be populated in the list.
            // Then it scrolls to the previously preserved position (after recreation of the
            // fragment).
            mThreadListAdapter.reserveItems(threadObservables.size());
            if (mThreadListState != null) {
                LinearLayoutManager layoutManager =
                        (LinearLayoutManager) mThreadList.getLayoutManager();
                layoutManager.onRestoreInstanceState(mThreadListState);
                mThreadListState = null;
            }

            c.threads = new HashMap<>(threadObservables.size()); // No sparse. We need #values().
            c.disposable = Observable.merge(threadObservables)
                    .observeOn(Schedulers.computation())
                    // Buffers updates together to avoid flickering effect.
                    .buffer(100, TimeUnit.MILLISECONDS)
                    .subscribe(threadResourceList -> {
                        boolean oneUpdated = false;
                        for (Resource<MessageThread> threadResource : threadResourceList) {
                            MessageThread thread = threadResource.data;
                            if (thread == null) {
                                continue;
                            }

                            c.threads.put(thread.id, thread);
                            oneUpdated = true;
                        }

                        if (!oneUpdated) {
                            return;
                        }

                        // Replaces the thread items.
                        mThreadListAdapter.replace(new ArrayList<>(c.threads.values()));
                    });
        });
    }

    @Override
    public void onPause() {
        mDisposable.dispose();

        LinearLayoutManager layoutManager = (LinearLayoutManager) mThreadList.getLayoutManager();
        mThreadListState = layoutManager.onSaveInstanceState();

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Saves the threadList's offset.
        outState.putParcelable(ICICLE_KEY_THREADS_STATE, mThreadListState);
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.navigation_item_messages);
    }
}
