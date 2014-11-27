package fi.bitrite.android.ws.activity;

import android.accounts.Account;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.auth.AuthenticationHelper;
import fi.bitrite.android.ws.messaging.RestUnreadCount;
import fi.bitrite.android.ws.util.GlobalInfo;
import fi.bitrite.android.ws.util.http.HttpException;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class MessagesTabActivity extends RoboActivity implements View.OnClickListener {

    @InjectView(R.id.unreadCount)
    TextView unreadCount;

    @InjectView(R.id.btnUpdateMessages)
    Button updateMessages;

    private DialogHandler dialogHandler;
    private int numUnread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messages_tab);

        dialogHandler = new DialogHandler(this);
        downloadUnreadCount();

        updateMessages.setOnClickListener(this);

        Button viewMessagesOnSite = (Button)findViewById(R.id.btnViewOnSite);
        viewMessagesOnSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Getting the uid out of the stashed cookie info is pretty sad. Maybe @jstaffans
                // can show me how to get it from the account object.
                SharedPreferences authInfo = getSharedPreferences("auth_cookie", 0);
                if (authInfo.contains("account_uid")) {
                    int uid = authInfo.getInt("account_uid", 0);
                    String url = GlobalInfo.warmshowersBaseUrl + "/user/" + uid + "/messages";
                    WebViewActivity.viewOnSite(MessagesTabActivity.this, url, getString(R.string.messages_on_site));
                }
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
    public void onClick(View view) {
        downloadUnreadCount();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (DialogHandler.inProgress()) {
            dialogHandler.dismiss();
            dialogHandler.showDialog(DialogHandler.MESSAGES);
        }
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
                // TODO: Improve error reporting with more specifics
                int r = (result instanceof HttpException ? R.string.network_error : R.string.error_retrieving_messages);
                dialogHandler.alert(getResources().getString(r));
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

        unreadCount.setText(text);
        updateMessages.setVisibility(View.VISIBLE);
    }
}
