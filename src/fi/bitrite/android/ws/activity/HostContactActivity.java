package fi.bitrite.android.ws.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.host.HostContact;
import fi.bitrite.android.ws.host.impl.RestHostContact;
import fi.bitrite.android.ws.model.Host;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import roboguice.util.Strings;

/**
 * Responsible for letting the user type in a message and then sending it to a host
 * over the WarmShowers web service.
 */
public class HostContactActivity extends RoboActivity {
    
    @InjectView(R.id.txtContactHostTitle) TextView title;
    @InjectView(R.id.editContactHostSubject) EditText editSubject;
    @InjectView(R.id.editContactHostMessage) EditText editMessage;
    
    private Host host;
    private DialogHandler dialogHandler;
    private HostContactTask hostContactTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.host_contact);

        dialogHandler = new DialogHandler(this);

        if (savedInstanceState != null) {
            host = savedInstanceState.getParcelable("host");
        } else {
            Intent i = getIntent();
            host = (Host) i.getParcelableExtra("host");
        }

        title.setText("Message to " + host.getFullname());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("host", host);
        super.onSaveInstanceState(outState);
    }

    public void sendMessageToHost(View view) {
        String subject = editSubject.getText().toString();
        String message = editMessage.getText().toString();

        if (Strings.isEmpty(subject) || Strings.isEmpty(message)) {
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
                HostContact contact = new RestHostContact();
                contact.send(host.getName(), subject, message);
            }

            catch (Exception e) {
                Log.e(WSAndroidApplication.TAG, e.getMessage(), e);
                retObj = e;
            }
            
            return retObj;
        }
        
        @Override
        protected void onPostExecute(Object result) {
            dialogHandler.dismiss();
            
            if (result instanceof Exception) {
                dialogHandler.alert(getResources().getString(R.string.error_sending_message) + " (" + ((Exception) result).getMessage() + ")");
            }
            else {
                showSuccessDialog();
            }
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
    
}
