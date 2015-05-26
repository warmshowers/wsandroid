package fi.bitrite.android.ws.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.RestClient;
import fi.bitrite.android.ws.host.impl.RestHostContact;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.util.Tools;
import fi.bitrite.android.ws.util.http.HttpException;

/**
 * Responsible for letting the user type in a message and then sending it to a host
 * over the WarmShowers web service.
 */
public class HostContactActivity extends WSBaseActivity
        implements android.widget.AdapterView.OnItemClickListener {

    TextView title;
    EditText editSubject;
    EditText editMessage;
    Button btnHostContact;
    TextView noNetworkWarning;

    private Host host;
    private DialogHandler dialogHandler;
    private HostContactTask hostContactTask;

    @Override
    protected void onResume() {
        super.onResume();
        if (!Tools.isNetworkConnected(this)) {
            noNetworkWarning.setText(getString(R.string.not_connected_to_network));
            btnHostContact.setEnabled(false);
            return;
        }
        btnHostContact.setEnabled(true);
        noNetworkWarning.setVisibility(View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.host_contact);

        // Tell the BaseActivity to use a back action in the toolbar instead of the hamburger
        hasBackIntent = true;

        initView();

        title = (TextView) findViewById(R.id.txtContactHostTitle);
        editSubject = (EditText) findViewById(R.id.editContactHostSubject);
        editMessage = (EditText) findViewById(R.id.editContactHostMessage);
        btnHostContact = (Button) findViewById(R.id.btnHostContact);
        noNetworkWarning = (TextView) findViewById(R.id.noNetworkWarningContact);

        dialogHandler = new DialogHandler(this);

        if (savedInstanceState != null) {
            host = savedInstanceState.getParcelable("host");
        } else {
            Intent i = getIntent();
            host = (Host) i.getParcelableExtra("host");
        }

        title.setText(getString(R.string.contact_message_to, host.getFullname()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("host", host);
        super.onSaveInstanceState(outState);
    }

    public void sendMessageToHost(View view) {
        String subject = editSubject.getText().toString();
        String message = editMessage.getText().toString();

        if (subject.isEmpty() || message.isEmpty()) {
            dialogHandler.alert(getResources().getString(R.string.message_validation_error));
            return;
        }

        dialogHandler.showDialog(DialogHandler.HOST_CONTACT);

        hostContactTask = new HostContactTask();
        hostContactTask.execute(subject, message);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        return dialogHandler.createDialog(id, getResources().getString(R.string.sending_message));
    }

    private class HostContactTask extends AsyncTask<String, Void, Object> {

        @Override
        protected Object doInBackground(String... params) {
            String subject = params[0];
            String message = params[1];
            Object retObj = null;
            try {
                RestHostContact contact = new RestHostContact();
                JSONObject result = contact.send(host.getName(), subject, message);

                JSONArray resultArray = result.getJSONArray("arrayresult");

                if (!resultArray.getBoolean(0)) {
                    throw new HttpException("Failed to send contact request, inappropriate result: " + resultArray);
                }
            } catch (Exception e) {
                Log.e(WSAndroidApplication.TAG, e.getMessage(), e);
                retObj = e;
            }

            return retObj;
        }

        @Override
        protected void onPostExecute(Object result) {
            dialogHandler.dismiss();
            if (result instanceof Exception) {
                RestClient.reportError(HostContactActivity.this, result);
                return;
            }
            showSuccessDialog();
        }
    }

    protected void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HostContactActivity.this);
        builder.setMessage(getResources().getString(R.string.message_sent)).setPositiveButton(
                getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
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
