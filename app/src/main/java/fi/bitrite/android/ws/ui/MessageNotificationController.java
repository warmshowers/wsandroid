package fi.bitrite.android.ws.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseIntArray;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.Message;
import fi.bitrite.android.ws.model.MessageThread;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.MessageRepository;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.listadapter.MessageListAdapter;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import fi.bitrite.android.ws.util.WSGlide;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.SerialDisposable;
import io.reactivex.schedulers.Schedulers;

@AccountScope
public class MessageNotificationController {
    private final static String CHANNEL_ID = "ws_messages";

    private final Context mApplicationContext;
    private final MessageRepository mMessageRepository;
    private final UserRepository mUserRepository;

    private final NotificationManager mNotificationManager;
    private final SparseIntArray mLastShownMessageIdByThreadId = new SparseIntArray();

    /**
     * This is true until we see a thread id for the second time. That is used to realize the moment
     * where the initial load from the db is done and new messages are coming from the network. Only
     * after that vibration is added to notifications.
     * Note: This misbehaves if no messages are stored in the db, so the first new message gets no
     *       vibrations. We currently accept this.
     */
    private boolean mMessagesAreComingFromDb = true;

    @Inject
    MessageNotificationController(
            Context applicationContext, LoggedInUserHelper loggedInUserHelper,
            MessageRepository messageRepository, UserRepository userRepository,
            @Named("accountDestructor") CompositeDisposable accountDestructor) {
        mApplicationContext = applicationContext;
        mMessageRepository = messageRepository;
        mUserRepository = userRepository;

        mNotificationManager = (NotificationManager) mApplicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        // Registers for updates from the message repository. We retrieve an observable list of
        // observables. As soon as the list changes, we no longer listen to changes of the old one
        // and re-register ourselves to all the observables of the new list.
        final SerialDisposable threadListDisposable = new SerialDisposable();
        Disposable repositoryDisposable = mMessageRepository.getAll().subscribe(observables -> {
            Disposable disposable = handleNewThreadList(observables);
            threadListDisposable.set(disposable);
        });

        accountDestructor.add(new Disposable() {
            private boolean mDisposed = false;

            @Override
            public void dispose() {
                mDisposed = true;
                repositoryDisposable.dispose();
                threadListDisposable.dispose();
                dismissAllNotifications();
            }
            @Override
            public boolean isDisposed() {
                return mDisposed;
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
            || mNotificationManager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        String channelName = mApplicationContext.getString(R.string.notification_channel_messages_label);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        mNotificationManager.createNotificationChannel(channel);
    }

    private Disposable handleNewThreadList(List<Observable<Resource<MessageThread>>> observables) {
        ConcurrentSkipListSet<Integer > seenThreadIds = new ConcurrentSkipListSet<>();
        return Observable.mergeDelayError(observables)
                .observeOn(Schedulers.computation())
                .filter(Resource::hasData)
                .map(resource -> resource.data)
                .map(thread -> {
                    mMessagesAreComingFromDb = mMessagesAreComingFromDb && seenThreadIds.add(thread.id);
                    return thread;
                })
                .flatMapCompletable(this::handleThreadUpdate)
                .subscribe();
    }

    private Completable handleThreadUpdate(@NonNull MessageThread thread) {
        boolean hasNew = thread.hasNewMessages();
        if (!hasNew) {
            // Remove any existing notification.
            mNotificationManager.cancel(thread.id);
            return Completable.complete();
        }

        NotificationHelper helper = new NotificationHelper(thread);
        if (helper.latestNewMessage == null) {
            mNotificationManager.cancel(thread.id);
            return Completable.complete();
        }

        // Only threads with new messages from here on.
        synchronized (mLastShownMessageIdByThreadId) {
            if (helper.latestNewMessage.id == mLastShownMessageIdByThreadId.get(thread.id)) {
                return Completable.complete();
            }
        }

        return Single.just(helper)
                .flatMap(NotificationHelper::loadParticipants)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(NotificationHelper::loadPartnerBitmap)
                .map(h -> {
                    updateNotification(h);
                    return h;
                })
                .toCompletable();
    }

    private void updateNotification(NotificationHelper helper) {
        // Removes the notification if nothing is to be notified about.
        final int notificationId = helper.thread.id;
        if (helper.latestNewMessage == null) {
            mNotificationManager.cancel(notificationId);
            return;
        }

        Intent resultIntent = MainActivity.createForMessageThread(
                mApplicationContext, helper.thread.id);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your app to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mApplicationContext);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (Message message : helper.thread.messages) {
            if (!message.isNew) {
                continue;
            }
            style.addLine(message.body);
        }
        User participant = helper.participants.get(helper.latestNewMessage.authorId);
        String newestMessageAuthorName = participant != null ? participant.getName() : "";

        Notification notification =
                new NotificationCompat.Builder(mApplicationContext, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_bicycle_white_24dp)
                        .setLargeIcon(helper.partnerProfileBitmap)
                        .setStyle(style)
                        .setContentTitle(newestMessageAuthorName)
                        .setContentText(helper.latestNewMessage.body)
                        .setContentIntent(resultPendingIntent)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(mMessagesAreComingFromDb
                                ? Notification.PRIORITY_LOW
                                : Notification.PRIORITY_HIGH)
                        .build();
        synchronized (mLastShownMessageIdByThreadId) {
            mLastShownMessageIdByThreadId.put(helper.thread.id, helper.latestNewMessage.id);
            mNotificationManager.notify(notificationId, notification);
        }
    }

    private void dismissAllNotifications() {
        mNotificationManager.cancelAll();
    }

    /**
     * Per-thread structure that keeps required data for a notification around.
     */
    private class NotificationHelper {
        @NonNull MessageThread thread;
        @Nullable Message latestNewMessage;
        @NonNull Map<Integer, User> participants = Collections.emptyMap();
        @Nullable Bitmap partnerProfileBitmap;

        NotificationHelper(@NonNull MessageThread thread) {
            List<Message> sortedMessages = new ArrayList<>(thread.messages);
            Collections.sort(sortedMessages, MessageListAdapter.COMPARATOR);
            this.thread = new MessageThread(
                    thread.id, thread.subject, thread.started, thread.isRead,
                    thread.participantIds, sortedMessages, thread.lastUpdated);

            // Find latestNewMessage.
            for (Message message : thread.messages) {
                if (message.isNew) {
                    latestNewMessage = message;
                }
            }
        }

        /**
         * Tries to fetch the participants of the message thread from the user repository and loads
         * them into @participants.
         *
         * @return
         *      A Completable that completes as soon as the attempt to load the participants
         *      finished. Never returns an error.
         */
        Single<NotificationHelper> loadParticipants() {
            assert latestNewMessage != null;

            // Loads the author of the message.
            List<Maybe<User>> toBeFetchedParticipantsRx =
                    new ArrayList<>(thread.participantIds.size());
            for (Integer participantId : thread.participantIds) {
                toBeFetchedParticipantsRx.add(mUserRepository.get(participantId)
                        .filter(Resource::hasData)
                        .map(userResource -> userResource.data)
                        .firstElement()
                        .onErrorComplete());
            }

            return Maybe.mergeDelayError(toBeFetchedParticipantsRx)
                    .reduceWith(() -> new HashMap<Integer, User>(thread.participantIds.size()),
                            (participants, user) -> {
                                participants.put(user.id, user);
                                return participants;
                            })
                    .map(participants -> {
                        this.participants = Collections.unmodifiableMap(participants);
                        return this;
                    });
        }

        Single<NotificationHelper> loadPartnerBitmap() {
            assert latestNewMessage != null;

            Single<NotificationHelper> justThis = Single.just(this);
            if (thread.participantIds.size() > 2) {
                // Group chat. We do not show any bitmap.
                return justThis;
            }

            User partner = participants.get(latestNewMessage.authorId);
            if (partner == null) {
                // We were not able to load the partner user. The notification can be shown
                // nevertheless.
                return justThis;
            }

            String pictureUrl = partner.profilePicture.getSmallUrl();
            if (TextUtils.isEmpty(pictureUrl)) {
                return justThis;
            }

            return Single.create(e -> {
                // Gets the partner (not our) user element.
                Target<Bitmap> target = new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap,
                                                @Nullable Transition<? super Bitmap> transition) {
                        partnerProfileBitmap = bitmap;
                        e.onSuccess(NotificationHelper.this);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // We still signal success. The notification can be shown without the
                        // bitmap.
                        e.onSuccess(NotificationHelper.this);
                    }
                };

                WSGlide.with(mApplicationContext)
                        .asBitmap()
                        .load(pictureUrl)
                        .into(target);
            });
        }
    }
}
