package fi.bitrite.android.ws;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import io.reactivex.disposables.Disposable;

/**
 * Runs the reloading of messages in a scheduled job service. That uses less battery power than
 * using a traditional service which is running all the time in the background.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AutoMessageReloadJobService extends JobService {
    private final static String TAG = AutoMessageReloadJobService.class.getCanonicalName();
    private final static int JOB_ID_RELOAD_MESSAGES = 1;

    @Inject AutoMessageReloadScheduler mAutoMessageReloadScheduler;

    private Disposable mDisposable;

    public static void reschedule(Context context,
                                  long messageReloadIntervalMs,
                                  AutoMessageReloadScheduler autoMessageReloadScheduler) {
        Log.d(TAG, String.format("reschedule: %dms", messageReloadIntervalMs));
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }

        if (messageReloadIntervalMs > 0) {
            // Create the job description.
            ComponentName componentName =
                    new ComponentName(context, AutoMessageReloadJobService.class);
            JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(JOB_ID_RELOAD_MESSAGES, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                jobInfoBuilder.setPeriodic(messageReloadIntervalMs, 0);
            } else {
                jobInfoBuilder.setPeriodic(messageReloadIntervalMs);
            }
            JobInfo newReloadMessagesJob = jobInfoBuilder.build();

            // Check whether a job with the correct periodicity is already scheduled.
            JobInfo reloadMessagesJob = findScheduledReloadMessagesJob(jobScheduler);
            if (reloadMessagesJob != null
                && reloadMessagesJob.getIntervalMillis() == newReloadMessagesJob.getIntervalMillis()) {
                return;
            }

            // Immediately trigger a reload.
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    autoMessageReloadScheduler.reloadMessagesInAllAccounts()
                            .onErrorComplete()
                            .subscribe(() -> {
                                Log.d(TAG, "Initial auto-reloading messages completed");
                            });
                }
            }, 100);
            // We might be on the DI stack. Trigger the initial loading after completing that.

            Log.d(TAG, "Scheduling message reload job");
            int resultCode = jobScheduler.schedule(jobInfoBuilder.build());
            if (resultCode != JobScheduler.RESULT_SUCCESS) {
                Log.e(TAG, "Message reload job failed to be scheduled.");
            }
        } else {
            jobScheduler.cancel(JOB_ID_RELOAD_MESSAGES);
        }
    }

    @Nullable
    private static JobInfo findScheduledReloadMessagesJob(@NonNull JobScheduler jobScheduler) {
        List<JobInfo> scheduledJobs = jobScheduler.getAllPendingJobs();
        for (JobInfo jobInfo : scheduledJobs) {
            if (jobInfo.getId() == JOB_ID_RELOAD_MESSAGES) {
                return jobInfo;
            }
        }
        return null;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        WSAndroidApplication.getAppComponent().inject(this);

        Log.d(TAG, "Auto-reloading messages");

        mDisposable = mAutoMessageReloadScheduler.reloadMessagesInAllAccounts()
                .onErrorComplete()
                .subscribe(() -> {
                    mDisposable = null;

                    Log.d(TAG, "Auto-reloading messages completed");

                    // Mark the job as finished and do NOT request a reschedule. A reschedule should
                    // take place in case of an error and has nothing to do with repeated job
                    // execution. It would apply back-off policy for the job.
                    jobFinished(jobParameters, false);
                });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Auto-reload messages job stopped.");
        if (mDisposable != null) {
            mDisposable.dispose();
            mDisposable = null;
        }

        // Try to reschedule. This is never done in case of the reload time being zero.
        return true;
    }
}
