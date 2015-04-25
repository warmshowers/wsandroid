package fi.bitrite.android.ws.activity;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.messaging.RestUnreadCount;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.Tools;



public class MessagesTabActivity extends WSBaseActivity implements android.widget.AdapterView.OnItemClickListener, View.OnClickListener {

    TextView mNoNetworkWarning;
    TextView mUnreadCount;
    Button mUpdateMessages;
    Button mViewMessagesOnSite;

    private DialogHandler dialogHandler;
    private int numUnread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messages_tab);
        initView();

        mNoNetworkWarning = (TextView)findViewById(R.id.noNetworkWarningMessages);
        mUnreadCount = (TextView)findViewById(R.id.unreadCount);
        mUpdateMessages = (Button)findViewById(R.id.btnUpdateMessages);
        mViewMessagesOnSite = (Button)findViewById(R.id.btnViewOnSite);

        dialogHandler = new DialogHandler(this);
        if (Tools.isNetworkConnected(this)) {
            downloadUnreadCount();
        }

        mUpdateMessages.setOnClickListener(this);

        mViewMessagesOnSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = GlobalInfo.warmshowersBaseUrl + "/user/" + AuthenticationHelper.getAccountUid() + "/messages";
                WebViewActivity.viewOnSite(MessagesTabActivity.this, url, getString(R.string.messages_on_site));
            }
        });
    }

    private void downloadUnreadCount() {
        dialogHandler.showDialog(DialogHandler.MESSAGES);
        MessagesTask messagesTask = new MessagesTask();
        messagesTask.execute();
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogHandler.inProgress()) {
            return dialogHandler.createDialog(id, getResources().getString(R.string.messages_in_progress));
        } else {
            return null;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!Tools.isNetworkConnected(this)) {
            mNoNetworkWarning.setText(getString(R.string.not_connected_to_network));
            mUpdateMessages.setEnabled(false);
            mViewMessagesOnSite.setEnabled(false);
            return;
        }
        mUpdateMessages.setEnabled(true);
        mUpdateMessages.setVisibility(View.VISIBLE);
        mViewMessagesOnSite.setEnabled(true);

        mNoNetworkWarning.setText("");
        mNoNetworkWarning.setVisibility(View.GONE);

        if (DialogHandler.inProgress()) {
            dialogHandler.dismiss();
            dialogHandler.showDialog(DialogHandler.MESSAGES);
        }
    }

    @Override
    public void onClick(View v) {
        downloadUnreadCount();
    }

    private class MessagesTask extends AsyncTask<Void, Void, Object> {

        @Override
        protected Object doInBackground(Void... params) {
            Object retObj = null;

            try {
                RestUnreadCount unreadCount = new RestUnreadCount();
                numUnread = unreadCount.getUnreadCount();
            } catch (Exception e) {
                if (DialogHandler.inProgress()) {
                    dialogHandler.dismiss();
                }
                Log.e(WSAndroidApplication.TAG, e.getMessage(), e);
                retObj = e;
            }

            return retObj;
        }

        @Override
        protected void onPostExecute(Object result) {
            dialogHandler.dismiss();

            if (result instanceof Exception) {
                RestClient.reportError(MessagesTabActivity.this, result);
                return;
            }

            updateViewContent();
        }
    }

    private void updateViewContent() {
        String text;

        if (numUnread > 0) {
            String numUnreadFormat = getResources().getString(R.string.unread_messages);
            text = String.format(numUnreadFormat, numUnread);
        } else {
            text = getResources().getString(R.string.no_unread_messages);
        }

        mUnreadCount.setText(text);
        mUpdateMessages.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

}

