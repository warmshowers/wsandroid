package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.model.MessageThread;
import fi.bitrite.android.ws.repository.MessageRepository;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.listadapter.MessageListAdapter;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import io.reactivex.CompletableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class MessageThreadFragment extends BaseFragment {

    private final static String KEY_THREAD_ID = "thread_id";

    @Inject LoggedInUserHelper mLoggedInUserHelper;
    @Inject MessageRepository mMessageRepository;
    @Inject UserRepository mUserRepository;

    @BindView(R.id.thread_lst_messages) RecyclerView mLstMessage;
    @BindView(R.id.thread_edt_new_message) EditText mEdtNewMessage;
    @BindView(R.id.all_btn_submit) ImageButton mBtnSubmit;

    private int mThreadId;
    private Disposable mDisposable;
    private MessageListAdapter mMessageListAdapter;

    public static Fragment create(int threadId) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_THREAD_ID, threadId);

        Fragment fragment = new MessageThreadFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mThreadId = getArguments().getInt(KEY_THREAD_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message_thread, container, false);
        ButterKnife.bind(this, view);

        // Marks the thread as read.
        mMessageRepository.markThreadAsRead(mThreadId)
                .onErrorComplete() // TODO(saemy): Error handling.
                .subscribe();

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMessageListAdapter = new MessageListAdapter(mLoggedInUserHelper, mUserRepository);
        mLstMessage.setAdapter(mMessageListAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        mDisposable = mMessageRepository.get(mThreadId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(resource -> {
                    MessageThread thread = resource.data;
                    if (thread == null) {
                        return;
                    }

                    // Forwards the messages to the list adapter.
                    mMessageListAdapter.replaceRx(thread.messages)
                            .observeOn(AndroidSchedulers.mainThread())
                            .andThen((CompletableSource) emitter -> {
                                mLstMessage.scrollToPosition(thread.messages.size() - 1);
                                emitter.onComplete();
                            })
                            .subscribe();

                    // Sets the title.
                    setTitle(thread.subject);
                });
    }

    @Override
    public void onPause() {
        mDisposable.dispose();

        super.onPause();
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.navigation_item_messages);
    }

    @OnClick(R.id.all_btn_submit)
    void onSendMessage() {
        String message = mEdtNewMessage.getText().toString();
        if (TextUtils.isEmpty(message)) {
            // Just do nothing.
            return;
        }

        mMessageRepository.sendMessage(mThreadId, message)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> mEdtNewMessage.setText(""),
                        throwable -> {
                            // This should not happen, if there is no network connection we schedule
                            // sending the message for when it is back.
                            Toast.makeText(getContext(), R.string.message_send_failed,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        });
    }
}

