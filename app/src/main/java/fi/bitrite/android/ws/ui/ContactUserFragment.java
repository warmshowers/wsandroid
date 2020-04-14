package fi.bitrite.android.ws.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.helper.HttpErrorHelper;
import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.repository.MessageRepository;
import fi.bitrite.android.ws.ui.util.DialogHelper;
import fi.bitrite.android.ws.ui.util.NavigationController;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.util.Tools;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Responsible for letting the user type in a message and then sending it to an other user
 * over the Warmshowers web service.
 */
public class ContactUserFragment extends BaseFragment {

    private final static String KEY_RECIPIENT_USERNAME = "recipient_name";
    private final static String KEY_RECIPIENT_FULLNAME = "recipient_fullname";

    @Inject MessageRepository mMessageRepository;

    @BindView(R.id.contact_user_txt_subject) EditText mTxtSubject;
    @BindView(R.id.contact_user_txt_message) EditText mTxtMessage;
    @BindView(R.id.all_btn_submit) Button mBtnSubmit;
    @BindView(R.id.all_lbl_no_network_warning) TextView mLblNoNetworkWarning;

    private String mRecipientName;
    private String mRecipientFullname;

    private Disposable mProgressDisposable;
    private BehaviorSubject<MessageSendResult> mLastMessageSendResult = BehaviorSubject.create();

    public static Fragment create(SimpleUser recipient) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_RECIPIENT_USERNAME, recipient.username);
        bundle.putString(KEY_RECIPIENT_FULLNAME, recipient.fullname);

        Fragment fragment = new ContactUserFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_user, container, false);
        ButterKnife.bind(this, view);

        mRecipientName = getArguments().getString(KEY_RECIPIENT_USERNAME);
        mRecipientFullname = getArguments().getString(KEY_RECIPIENT_FULLNAME);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isConnected = Tools.isNetworkConnected(getContext());
        mLblNoNetworkWarning.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        mBtnSubmit.setEnabled(isConnected);

        // Structure for decoupling the message send callback that is processed upon arrival from
        // its handler that can only be executed when the app is in the foreground. Handler.
        getResumePauseDisposable().add(mLastMessageSendResult
                .filter(result -> !result.isHandled)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    result.isHandled = true;

                    mProgressDisposable.dispose();
                    if (result.threadId != null) {
                        // The back action should take us from the message thread to the caller of this
                        // fragment and not this form fragment itself.
                        final NavigationController navigationController = getNavigationController();
                        navigationController.popBackStack();

                        if (result.threadId != MessageRepository.STATUS_NEW_THREAD_ID_NOT_IDENTIFIABLE) {
                            // This is a valid threadId.
                            navigationController.navigateToMessageThread(result.threadId);
                        } else {
                            // We just navigate to the thread list...
                            navigationController.navigateToMessageThreads();
                        }
                    } else {
                        HttpErrorHelper.showErrorToast(getContext(), result.throwable);
                    }
                }));
    }

    @OnClick(R.id.all_btn_submit)
    public void sendMessageToUser() {
        String subject = mTxtSubject.getText().toString();
        String message = mTxtMessage.getText().toString();

        if (TextUtils.isEmpty(subject) || TextUtils.isEmpty(message)) {
            DialogHelper.alert(getContext(), R.string.message_validation_error);
            return;
        }

        mProgressDisposable = ProgressDialog.create(R.string.sending_message)
                .show(getActivity());

        // Structure for decoupling the message send callback that is processed upon arrival from
        // its handler that can only be executed when the app is in the foreground. Callback.
        List<String> recipients = Collections.singletonList(mRecipientName);
        Disposable unused = mMessageRepository
                .createThread(subject, message, recipients)
                // We wait for the threadId to become available.
                .filter(threadId -> threadId != MessageRepository.STATUS_NEW_THREAD_ID_NOT_YET_KNOWN)
                .subscribe(
                        threadId -> mLastMessageSendResult.onNext(new MessageSendResult(threadId)),
                        throwable -> {
                            Log.d(WSAndroidApplication.TAG,
                                    "Failed to create a new message thread: "
                                    + throwable.getMessage());
                            mLastMessageSendResult.onNext(new MessageSendResult(throwable));
                        }
                );
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_contact_user, mRecipientFullname);
    }

    private class MessageSendResult {
        final Integer threadId;
        final Throwable throwable;
        boolean isHandled = false;

        private MessageSendResult(@NonNull Integer threadId) {
            this.threadId = threadId;
            this.throwable = null;
        }
        private MessageSendResult(@NonNull Throwable throwable) {
            this.threadId = null;
            this.throwable = throwable;
        }
    }
}
