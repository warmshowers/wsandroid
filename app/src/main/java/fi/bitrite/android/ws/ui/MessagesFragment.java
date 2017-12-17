package fi.bitrite.android.ws.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.api_new.AuthenticationController;
import fi.bitrite.android.ws.messaging.RestUnreadCount;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import fi.bitrite.android.ws.util.Tools;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class MessagesFragment extends BaseFragment {

    @Inject LoggedInUserHelper mLoggedInUserHelper;
    @Inject AuthenticationController mAuthenticationController;

    @BindView(R.id.messages_lbl_no_network_warning) TextView mLblNoNetworkWarning;
    @BindView(R.id.messages_lbl_num_unread) TextView mLblUnreadCount;
    @BindView(R.id.messages_btn_update_messages) Button mBtnUpdateMessages;
    @BindView(R.id.messages_btn_view_on_site) Button mBtnViewMessgesOnSite;

    private BehaviorSubject<Integer> mNumUnread = BehaviorSubject.createDefault(0);
    private Disposable mNumUnreadDisposable;

    public static Fragment create() {
        Bundle bundle = new Bundle();

        Fragment fragment = new MessagesFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        ButterKnife.bind(this, view);

        if (Tools.isNetworkConnected(getContext())) {
            downloadUnreadCount();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mNumUnreadDisposable = mNumUnread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(numUnread -> {
                    String text = (numUnread > 0)
                            ? getResources().getQuantityString(R.plurals.unread_messages, numUnread, numUnread)
                            : getResources().getString(R.string.no_unread_messages);
                    mLblUnreadCount.setText(text);
                });

        boolean isNetworkConnected = Tools.isNetworkConnected(getContext());
        mLblNoNetworkWarning.setVisibility(isNetworkConnected ? View.GONE : View.VISIBLE);
        mBtnUpdateMessages.setEnabled(isNetworkConnected);
        mBtnViewMessgesOnSite.setEnabled(isNetworkConnected);
    }

    @Override
    public void onPause() {
        mNumUnreadDisposable.dispose();
        super.onPause();
    }

    private ProgressDialog.Disposable mProgressDisposable;

    @OnClick(R.id.messages_btn_update_messages)
    public void downloadUnreadCount() {
        mProgressDisposable = ProgressDialog.create(R.string.messages_in_progress)
                .show(getActivity());

        MessagesTask messagesTask = new MessagesTask();
        messagesTask.execute();
    }

    @OnClick(R.id.messages_btn_view_on_site)
    public void viewMessagesOnSite() {
        String url = GlobalInfo.warmshowersBaseUrl + "/user/" + mLoggedInUserHelper.get().getId() + "/messages";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }


    private class MessagesTask extends AsyncTask<Void, Void, Object> {

        @Override
        protected Object doInBackground(Void... params) {
            Object retObj = null;

            try {
                RestUnreadCount unreadCount = new RestUnreadCount(mAuthenticationController);
                mNumUnread.onNext(unreadCount.getUnreadCount());
            } catch (Exception e) {
                retObj = e;
            }

            return retObj;
        }

        @Override
        protected void onPostExecute(Object result) {
            mProgressDisposable.dispose();

            if (result instanceof Exception) {
                Exception e = (Exception) result;
                Log.e(WSAndroidApplication.TAG, e.getMessage(), e);
                RestClient.reportError(getContext(), result);
            }
        }
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.title_fragment_messages);
    }
}

