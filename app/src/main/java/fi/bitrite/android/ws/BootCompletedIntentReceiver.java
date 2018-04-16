package fi.bitrite.android.ws;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class BootCompletedIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TextUtils.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        // Start the WSAndroidService.
        // FIXME(saemy): Start in a JobScheduler as it needs to be started in the foreground?
        Intent serviceIntent = new Intent(context, WSAndroidService.class);
        context.startService(serviceIntent);

//        JobScheduler js = new JobScheduler() {
//            @Override public int schedule(@NonNull JobInfo jobInfo) {
//                return 0;
//            }
//
//            @Override
//            public int enqueue(@NonNull JobInfo jobInfo, @NonNull JobWorkItem jobWorkItem) {
//                return 0;
//            }
//
//            @Override public void cancel(int i) {
//
//            }
//
//            @Override public void cancelAll() {
//
//            }
//
//            @NonNull @Override public List<JobInfo> getAllPendingJobs() {
//                return null;
//            }
//
//            @Nullable @Override public JobInfo getPendingJob(int i) {
//                return null;
//            }
//        }
    }
}
