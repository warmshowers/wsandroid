package fi.bitrite.android.ws.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.Message;
import fi.bitrite.android.ws.model.MessageThread;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.repository.MessageRepository;
import fi.bitrite.android.ws.repository.Resource;
import fi.bitrite.android.ws.repository.UserRepository;
import fi.bitrite.android.ws.ui.listadapter.MessageListAdapter;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.SerialDisposable;
import io.reactivex.schedulers.Schedulers;

@AccountScope
public class MessageNotificationController {
    private final static String CHANNEL_ID = "ws_messages";
    private final static String NOTIFICATION_GROUP = CHANNEL_ID;

    private final Context mApplicationContext;
    private final LoggedInUserHelper mLoggedInUserHelper;
    private final MessageRepository mMessageRepository;
    private final UserRepository mUserRepository;

    private final NotificationManager mNotificationManager;

    private final SparseArray<NotificationEntry> mNotificationsByThread = new SparseArray<>();

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
        mLoggedInUserHelper = loggedInUserHelper;
        mMessageRepository = messageRepository;
        mUserRepository = userRepository;

        mNotificationManager = (NotificationManager) mApplicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE);

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

    private Disposable handleNewThreadList(List<Observable<Resource<MessageThread>>> observables) {
        Set<Integer> seenThreadIds = new HashSet<>();
        return Observable.mergeDelayError(observables)
                .observeOn(Schedulers.computation())
                .filter(Resource::hasData)
                .map(resource -> resource.data)
                .filter(thread -> {
                    mMessagesAreComingFromDb =
                            mMessagesAreComingFromDb && seenThreadIds.add(thread.id);

                    boolean hasNew = thread.hasNewMessages();
                    if (!hasNew) {
                        // Remove any existing notification.
                        NotificationEntry entry = mNotificationsByThread.get(thread.id);
                        if (entry != null) {
                            dismissNotification(entry);
                        }
                    }
                    return hasNew;
                })
                // Only unread threads from here.
                .map(thread -> {
                    Collections.sort(thread.messages, MessageListAdapter.COMPARATOR);
                    return thread;
                })
                .map(this::getNotificationEntry)
                .flatMap(this::loadParticipantsIntoNotificationEntry)
                .observeOn(AndroidSchedulers.mainThread()) // Why is this needed Picasso?
                .flatMap(this::loadPartnerBitmapIntoNotificationEntry)
//                .debounce(1, TimeUnit.SECONDS) // Limit number of updates during burst. FIXME: does not deliver all the entries
                .subscribe(this::updateNotification, e -> {
                    // TODO(saemy): Error handling. E.g. when loading a participant fails.
                    Log.e(WSAndroidApplication.TAG, e.toString());
                });
    }

    /**
     * Returns the notification entry for given threaad.
     */
    @NonNull
    private NotificationEntry getNotificationEntry(MessageThread thread) {
        NotificationEntry entry = mNotificationsByThread.get(thread.id);
        if (entry == null) {
            entry = new NotificationEntry(thread);
            mNotificationsByThread.put(thread.id, entry);
        } else {
            entry.setThread(thread); // Thread objects are subject to change.
        }
        return entry;
    }

    /**
     * Checks whether the author needs to be fetched from the users repository for the given
     * notification entry
     * @return An observable (actually a single) that fires as soon as the entry contains the user.
     */
    private Observable<NotificationEntry> loadParticipantsIntoNotificationEntry(
            NotificationEntry entry) {
        // Loads the author of the message.
        SparseArray<User> participants = entry.participants;

        Set<Integer> toBeFetchedParticipantIds = new HashSet<>(participants.size());
        for (Integer participantId : entry.thread.participantIds) {
            if (participants.get(participantId) == null) {
                toBeFetchedParticipantIds.add(participantId);
            }
        }

        if (!toBeFetchedParticipantIds.isEmpty()) {
            // Fetches the participating users from the repository.
            return Observable.mergeDelayError(mUserRepository.get(toBeFetchedParticipantIds))
                    .filter(Resource::hasData)
                    .map(userResource -> userResource.data)
                    .map(user -> {
                        entry.participants.put(user.id, user);
                        toBeFetchedParticipantIds.remove(user.id);
                        return entry;
                    })
                    // Only fire once we loaded all participants.
                    .filter(e -> toBeFetchedParticipantIds.isEmpty());
        } else {
            return Observable.just(entry);
        }
    }

    private Observable<NotificationEntry> loadPartnerBitmapIntoNotificationEntry(
            NotificationEntry entry) {
        return Observable.create(e -> {
            if (entry.partnerProfileBitmap != null || entry.participants.size() != 2) {
                e.onNext(entry);
                return;
            }

            // Gets the partner (not our) user element.
            User partner = entry.participants.valueAt(0);
            if (partner.id == mLoggedInUserHelper.getId()) {
                partner = entry.participants.valueAt(1);
            }

            String pictureUrl = partner.profilePicture.getSmallUrl();
            if (!TextUtils.isEmpty(pictureUrl)) {
                Target target = new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        entry.partnerProfileBitmap = bitmap;
                        e.onNext(entry);
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                        // Ignore the error.
                        e.onNext(entry);
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                };

                Picasso.with(mApplicationContext)
                        .load(pictureUrl)
                        .into(target);
            } else {
                e.onNext(entry);
            }
        });
    }

    private void updateNotification(NotificationEntry entry) {
        // Removes the notification if nothing is to be notified about.
        if (entry.latestNewMessage == null) {
            dismissNotification(entry);
            return;
        }
        if (entry.lastShownNewMessageId != null
            && entry.latestNewMessage.id == entry.lastShownNewMessageId) {
            // See the doc of {@link NotificationEntry::lastShownNewMessageId} for why we are doing
            // this.
            return;
        }
        entry.lastShownNewMessageId = entry.latestNewMessage.id;

        Intent resultIntent = MainActivity.createForMessageThread(
                mApplicationContext, entry.thread.id);

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

        Notification notification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? createNotification(entry, resultPendingIntent)
                : createNotificationBeforeApi24(entry, resultPendingIntent);

        if (!mMessagesAreComingFromDb) {
            notification.defaults |= Notification.DEFAULT_ALL;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notification.priority |= Notification.PRIORITY_HIGH;
            }
        }

        mNotificationManager.notify(entry.notificationId(), notification);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Notification createNotification(NotificationEntry entry, PendingIntent intent) {
        User us = mLoggedInUserHelper.get();
        if (us == null) {
            return createNotificationBeforeApi24(entry, intent);
        }

        Notification.MessagingStyle style = new Notification.MessagingStyle(us.fullname)
                .setConversationTitle(entry.thread.subject);
        for (Message message : entry.thread.messages) {
            User participant = entry.participants.get(message.authorId);
            String authorName = participant != null ? participant.fullname : "";
            Notification.MessagingStyle.Message msg = new Notification.MessagingStyle.Message(
                    message.body, message.date.getTime(), authorName);
            if (message.isNew) {
                style.addMessage(msg);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                style.addHistoricMessage(msg);
            }
        }

        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? new Notification.Builder(mApplicationContext, CHANNEL_ID)
                        : new Notification.Builder(mApplicationContext))
                .setSmallIcon(R.drawable.ic_bicycle_white_24dp)
                .setLargeIcon(entry.partnerProfileBitmap)
                .setStyle(style)
                .setContentIntent(intent)
                .setGroup(NOTIFICATION_GROUP)
                .build();
    }

    private Notification createNotificationBeforeApi24(NotificationEntry entry,
                                                       PendingIntent intent) {
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (Message message : entry.thread.messages) {
            if (!message.isNew) {
                continue;
            }
            style.addLine(message.body);
        }

        assert entry.latestNewMessage != null;
        User participant = entry.participants.get(entry.latestNewMessage.authorId);
        String newestMessageAuthorName = participant != null ? participant.fullname : "";
        return new NotificationCompat.Builder(mApplicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bicycle_white_24dp)
                .setLargeIcon(entry.partnerProfileBitmap)
                .setStyle(style)
                .setContentTitle(newestMessageAuthorName)
                .setContentText(entry.latestNewMessage.body)
                .setContentIntent(intent)
                .setGroup(NOTIFICATION_GROUP)
                .build();
    }

    private void dismissNotification(@NonNull NotificationEntry entry) {
        mNotificationManager.cancel(entry.notificationId());
        mNotificationsByThread.remove(entry.thread.id);
    }

    private void dismissAllNotifications() {
        for (int i = 0; i < mNotificationsByThread.size(); ++i) {
            dismissNotification(mNotificationsByThread.valueAt(i));
        }
        mNotificationsByThread.clear();
    }

    /**
     * Per-thread structure that keeps required data for a notification around.
     */
    private static class NotificationEntry {
        @NonNull MessageThread thread;
        @Nullable Message latestNewMessage;
        @NonNull final SparseArray<User> participants = new SparseArray<>();
        @Nullable Bitmap partnerProfileBitmap;
        /**
         * The id of the newest new message that is currently shown in a notification. We are not
         * re-issuing any new notifications as long as the latestNewMessage.id equals this value.
         * That prevents the notification to be re-shown in case it got dismissed by the user
         * followed by a reload of the messages which might change the MessageThread instance.
         */
        @Nullable Integer lastShownNewMessageId;

        NotificationEntry(@NonNull MessageThread thread) {
            setThread(thread);
        }

        void setThread(@NonNull MessageThread thread) {
            this.thread = thread;
            setLatestNewMessage();
        }

        int notificationId() {
            // We just use the thread id as the notification id.
            return thread.id;
        }

        private void setLatestNewMessage() {
            latestNewMessage = null;
            for (Message message : thread.messages) {
                if (message.isNew) {
                    latestNewMessage = message;
                }
            }
        }
    }
}
