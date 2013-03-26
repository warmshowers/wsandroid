package fi.bitrite.android.ws.activity;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.inject.Inject;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.auth.http.HttpAuthenticator;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.messaging.RestUnreadCount;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class MessagesTabActivity extends RoboActivity implements View.OnClickListener {

    @Inject
    HttpAuthenticator authenticationService;
    @Inject
    HttpSessionContainer sessionContainer;

    @InjectView(R.id.unreadCount)
    TextView unreadCount;

    @InjectView(R.id.messagingHelp)
    TextView messagingHelp;

    @InjectView(R.id.btnUpdateMessages)
    Button updateMessages;

	private DialogHandler dialogHandler;
    private int numUnread;
    private MessagesTask messagesTask;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.messages_tab);

		dialogHandler = new DialogHandler(this);
        getUnreadCount();

        updateMessages.setOnClickListener(this);
	}

    private void getUnreadCount() {
        dialogHandler.showDialog(DialogHandler.MESSAGES);
        messagesTask = new MessagesTask();
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
        getUnreadCount();
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
                dialogHandler.alert(getResources().getString(R.string.error_retrieving_messages));
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

        messagingHelp.setVisibility(View.VISIBLE);
        messagingHelp.setText(Html.fromHtml(getResources().getString(R.string.visit_warmshowers_website)));
        messagingHelp.setMovementMethod(LinkMovementMethod.getInstance());

        updateMessages.setVisibility(View.VISIBLE);
    }
}
