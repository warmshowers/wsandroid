package fi.bitrite.android.ws.repository;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.api.WarmshowersService;
import fi.bitrite.android.ws.api.response.MessageThreadListResponse;
import fi.bitrite.android.ws.api.response.MessageThreadResponse;
import fi.bitrite.android.ws.model.Message;
import fi.bitrite.android.ws.model.MessageThread;
import fi.bitrite.android.ws.persistence.MessageDao;
import fi.bitrite.android.ws.util.LoggedInUserHelper;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * Acts as a intermediate to return messages from the database and in the background re-fetching
 * them from the web.
 */
@Singleton
public class MessageRepository extends Repository<MessageThread> {

    public final static int STATUS_NEW_THREAD_ID_NOT_YET_KNOWN = 0;
    public final static int STATUS_NEW_THREAD_ID_NOT_IDENTIFIABLE = -1;

    private final LoggedInUserHelper mLoggedInUserHelper;
    private final MessageDao mMessageDao;
    private final WarmshowersService mWarmshowersService;

    // Contains the threadId-messageId pairs that are currently syncing.
    private final ConcurrentSkipListSet<SyncingKey> mSyncingMessages =
            new ConcurrentSkipListSet<>();

    @Inject
    MessageRepository(MessageDao messageDao, LoggedInUserHelper loggedInUserHelper,
                      WarmshowersService warmshowersService) {
        mMessageDao = messageDao;
        mLoggedInUserHelper = loggedInUserHelper;
        mWarmshowersService = warmshowersService;

        // Initializes the repository by loading the threads from the db.
        Completable.complete().subscribeOn(Schedulers.io()).subscribe(() -> {
            List<MessageThread> threads = mMessageDao.loadAll();
            for (MessageThread thread : threads) {
                put(thread.id, Resource.loading(thread), Freshness.FRESH);
            }
        });
    }

    // Makes it public.
    @Override
    public Observable<List<Observable<Resource<MessageThread>>>> getAll() {
        return super.getAll();
    }
    public Observable<Resource<MessageThread>> get(int threadId) {
        return super.get(threadId, ShouldSaveInDb.YES);
    }

    /**
     * Reloads the threads from the webservice.
     */
    public Completable reloadThreads() {
        return mWarmshowersService.fetchMessageThreads()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(apiResponse -> {
                    if (!apiResponse.isSuccessful()) {
                        throw new Error(apiResponse.errorBody().toString());
                    }

                    MessageThreadListResponse responseBody = apiResponse.body();
                    processMessageThreadsUpdate(responseBody.messageThreads);
                    return 0; // Just return something
                }).ignoreElements();
    }

    /**
     * Creates a new message thread.
     *
     * It eventually returns the newly created threadId. However, as we currently do not directly
     * get it back from the webservice, we need to do some heuristics. And also need one more
     * round-trip on the network. Therefore, we first emit
     * {@link #STATUS_NEW_THREAD_ID_NOT_YET_KNOWN}, which signals a successful completion.
     * If you are not interested in the new threadId, you can carry on from there.
     * Eventually, we submit the new threadId in the same observable as a second value. Note, if we
     * were not able to determine the value we emit
     * {@link #STATUS_NEW_THREAD_ID_NOT_IDENTIFIABLE}.
     * After the first value is successfully returned, no more errors will follow.
     *
     * @param recipients The list of the participating usernames without us.
     */
    public Observable<Integer> createThread(String subject, String message,
                                            List<String> recipients) {
        return Observable.<Integer>create(emitter -> {
            String recipientNames = TextUtils.join(",", recipients);
            mWarmshowersService.createMessageThread(recipientNames, subject, message)
                    .subscribe(response -> {
                        if (!response.isSuccessful()) {
                            throw new Exception(response.errorBody().toString());
                        } else if (!response.body().isSuccessful) {
                            throw new Exception("Retreived an unsuccessful response.");
                        }

                        // This is the initial success message. We do not know any threadId yet.
                        emitter.onNext(STATUS_NEW_THREAD_ID_NOT_YET_KNOWN);

                        // Since we do not get the new threadId back, we need to reload all threads.
                        // We make a copy as we otherwise get the same instance again for
                        // newThreadIds.
                        final List<Integer> oldThreadIds = new ArrayList<>(getAllIdsRaw());
                        reloadThreads()
                                .onErrorComplete() // Ignore errors
                                .subscribe(() -> {
                                    // We try to get a difference of already known and new
                                    // threadIds.
                                    Set<Integer> newThreadIds = getAllIdsRaw();
                                    newThreadIds.removeAll(oldThreadIds);

                                    int theThreadId = STATUS_NEW_THREAD_ID_NOT_IDENTIFIABLE;
                                    for (Integer threadId : newThreadIds) {
                                        MessageThread thread = getRaw(threadId);
                                        if (subject.equals(thread.subject)) {
                                            // This seems to be our created thread.
                                            theThreadId = threadId;
                                            break;
                                        }
                                    }

                                    emitter.onNext(theThreadId);
                                    emitter.onComplete();
                                });
                    }, throwable -> {
                        Log.e(WSAndroidApplication.TAG,
                                "Could not chreate the thread: " + throwable.toString());
                        emitter.onError(throwable);
                    });
        }).subscribeOn(Schedulers.io());
    }
    public Completable sendMessage(int threadId, String body) {
        return Completable.create(emitter -> {
            // Creates and saves a new message.
            MessageThread thread = getRaw(threadId);
            if (thread == null) {
                throw new Error("The thread needs to already be in the cache.");
            }

            int id = getNextPendingMessageId(thread);
            int authorId = mLoggedInUserHelper.getId();
            if (authorId == -1) {
                throw new Error("No currently logged in user.");
            }

            // Creates a temporary message and saves it into the database. We need to clone the
            // thread to make it distinguishable when it is compared to stored instances (which end
            // up to be the same instance in case we do no clone). We also set the lastUpdated field
            // to now.
            Message message =
                    new Message(id, threadId, authorId, new Date(), body, Message.STATUS_OUTGOING);
            List<Message> messages = new ArrayList<>(thread.messages);
            messages.add(message);
            thread = new MessageThread(
                    thread.id, thread.subject, thread.started, thread.readStatus,
                    thread.participantIds, messages, new Date());

            // Saves the thread in the db.
            save(threadId, thread)
                    .blockingAwait(); // TODO(saemy): Remove blocking.

            // We persisted the message. This is enough for success. We eventually post it to the
            // server.
            emitter.onComplete();

            // Does an attempt to send the message to the server. There could already be older,
            // unpushed messages on this thread. We should then try to send them first to not
            // reorder them.
            sendMessagesToServer(thread);
        }).subscribeOn(Schedulers.io());
    }

    public Completable markThreadAsUnread(int threadId) {
        return setThreadReadStatus(threadId, true);
    }
    public Completable markThreadAsRead(int threadId) {
        return setThreadReadStatus(threadId, false);
    }

    /**
     * We update the read status in two steps. First we mark the message as intended to be the
     * requested status. Then we try to update the status on the webservice. This can fail if there
     * is e.g. no network connection. Only if the update is successful, we also update the flag in
     * the db. However, if the network update fails, we wait until a connection is up again. We then
     * sync the thread once more to catch updates which may have done to it over e.g. the website or
     * a new message arrived. Only if no update is seen, we try to push the flag again.
     *
     * TODO(saemy): Test this.
     * TODO(saemy): Listen to network changes.
     */
    private Completable setThreadReadStatus(int threadId, boolean isUnread) {
        return Completable.create(emitter -> {
            MessageThread thread = getRaw(threadId);
            if (thread == null) {
                throw new Exception("The thread must already be in the repository.");
            }
            if (thread.isUnread() == isUnread) {
                emitter.onComplete();
                return;
            }

            thread = cloneForReadStatus(thread, isUnread
                    ? MessageThread.STATUS_UNREAD_NOT_YET_PUSHED
                    : MessageThread.STATUS_READ_NOT_YET_PUSHED);

            save(thread.id, thread)
                    .concatWith(setRemoteThreadReadStatus(threadId, isUnread))
                    .subscribe(emitter::onComplete, emitter::onError);
        });
    }
    private Completable setRemoteThreadReadStatus(int threadId, boolean isUnread) {
        int status = isUnread
                ? WarmshowersService.MESSAGE_THREAD_STAUS_UNREAD
                : WarmshowersService.MESSAGE_THREAD_STAUS_READ;
        return mWarmshowersService.setMessageThreadReadStatus(threadId, status)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .andThen((CompletableSource) (emitter -> {
                    MessageThread thread = getRaw(threadId);
                    if (thread == null) {
                        emitter.onComplete();
                        return;
                    }
                    thread = cloneForReadStatus(thread, isUnread
                            ? MessageThread.STATUS_UNREAD
                            : MessageThread.STATUS_READ);
                    save(thread.id, thread)
                            .subscribe(emitter::onComplete, emitter::onError);
                }));
    }

    @Override
    void saveInDb(int id, @NonNull MessageThread thread) {
        mMessageDao.save(thread);
    }

    @Override
    Observable<LoadResult<MessageThread>> loadFromDb(int threadId) {
        return Single.<LoadResult<MessageThread>>create(emitter -> {
            MessageThread thread = mMessageDao.loadThread(threadId);
            emitter.onSuccess(new LoadResult<>(LoadResult.Source.DB, thread));
        }).subscribeOn(Schedulers.io()).toObservable();
    }

    @Override
    Observable<LoadResult<MessageThread>> loadFromNetwork(int threadId) {
        return mWarmshowersService.fetchMessageThread(threadId)
                .subscribeOn(Schedulers.io())
                .map(apiResponse -> {
                    if (!apiResponse.isSuccessful()) {
                        throw new Error(apiResponse.errorBody().toString());
                    }

                    MessageThreadResponse apiThread = apiResponse.body();

                    MessageThread currentThread = getRaw(threadId);
                    int unreadStatus = currentThread != null
                            ? currentThread.readStatus : MessageThread.STATUS_READ;
                    Date started = currentThread != null ? currentThread.started : new Date();
                    Date lastUpdated = currentThread != null
                            ? currentThread.lastUpdated
                            : new Date();

                    MessageThread newThread =
                            apiThread.toMessageThread(unreadStatus, started, lastUpdated);

                    // We need to attach all the temporary messages from the db.
                    if (currentThread != null) {
                        for (Message message : currentThread.messages) {
                            if (message.status == Message.STATUS_OUTGOING) {
                                newThread.messages.add(message);
                            }
                        }
                    }

                    // Deletes the pushed temporary messages as we just received the
                    // non-temporary ones.
                    mMessageDao.deletePushedTemporaryMessages(threadId);

                    return new LoadResult<>(LoadResult.Source.NETWORK, newThread);
                });
    }

    @WorkerThread
    private void processMessageThreadsUpdate(
            @NonNull List<MessageThreadListResponse.Thread> apiThreads) {
        // Processes the api threads.
        List<Integer> apiThreadIds = new ArrayList<>(apiThreads.size());
        for (MessageThreadListResponse.Thread apiThread : apiThreads) {
            apiThreadIds.add(apiThread.id);

            // Checks if the message thread has new information.
            // We use some heuristics here as the webservice does not change lastUpdated on e.g.
            // read status changes.
            MessageThread dbThread = getRaw(apiThread.id);
            boolean updateNeeded = dbThread == null // New thread
                                   || dbThread.isUnread() != apiThread.isUnread()
                                   || dbThread.lastUpdated.before(apiThread.lastUpdated)
                                   || !dbThread.subject.equals(apiThread.subject);
            if (updateNeeded) {
                // Pushes the current value to the cache. (This is needed since the API response for
                // the single thread does not include the 'started' and 'last_update' fields.
                // Therefore, we must populate them here. Also marks the cache entry as outdated.
                List<Message> messages = dbThread != null ? dbThread.messages : new ArrayList<>();
                MessageThread temporary = apiThread.toMessageThread(messages);

                // Refetches the thread.
                reloadThread(temporary.id, temporary);
            } else if (dbThread.readStatus == MessageThread.STATUS_READ_NOT_YET_PUSHED
                       || dbThread.readStatus == MessageThread.STATUS_UNREAD_NOT_YET_PUSHED) {
                // This is the followup of #setThreadReadStatus(). We must push the thread's
                // read status upstream now that we know that the network is around and no
                // update was made. If it fails again, we just push it the next time.
                setRemoteThreadReadStatus(
                        dbThread.id,
                        dbThread.readStatus == MessageThread.STATUS_UNREAD_NOT_YET_PUSHED);

            }

            // Attemts the next resend of a pending outgoing message.
            if (dbThread != null) {
                sendMessagesToServer(dbThread);
            }
        }

        // Remove no longer existing threads from the cache and db.
        popExcept(apiThreadIds);
        mMessageDao.deleteExcept(apiThreadIds);
    }

    /**
     * Creates a clone of the given thread except that the readStatus is changed.
     */
    private static MessageThread cloneForReadStatus(MessageThread thread, int newReadStatus) {
        return new MessageThread(
                thread.id, thread.subject, thread.started, newReadStatus, thread.participantIds,
                thread.messages, thread.lastUpdated);
    }

    /**
     * Evicts the given thread from the cache and starts a reload.
     */
    private Observable<Resource<MessageThread>> reloadThread(int threadId) {
        return reloadThread(threadId, getRaw(threadId));
    }
    private Observable<Resource<MessageThread>> reloadThread(
            int threadId, @Nullable MessageThread thread) {
        return reload(threadId, thread, ShouldSaveInDb.YES);
    }

    ///// Sending a message.

    /**
     * Looks through the given thread's messages and determines the next unused message id for a
     * pending message.
     */
    private static int getNextPendingMessageId(MessageThread thread) {
        int id = -1;
        for (Message message : thread.messages) {
            if (message.id <= id) {
                id = message.id - 1;
            }
        }
        return id;
    }

    @WorkerThread
    private void sendMessagesToServer(@NonNull MessageThread thread) {
        List<Completable> completables = new LinkedList<>();
        for (Message message : thread.messages) {
            if (message.status == Message.STATUS_OUTGOING) {
                // Sends the message.
                completables.add(sendMessageToServerRx(thread, message, false));
            }
        }

        if (completables.isEmpty()) {
            return;
        }

        Completable.concat(completables)
                // Ignores errors. However, aborts sending any more messages as soon as one failed.
                .onErrorComplete()
                .doFinally(() -> reloadThread(thread.id))
                .subscribe();
    }

    @WorkerThread
    private Completable sendMessageToServerRx(MessageThread thread, Message message,
                                              boolean reloadThread) {
        return Completable.create(emitter -> {
            SyncingKey syncingKey = new SyncingKey(thread.id, message.id);
            boolean isNewEntry = mSyncingMessages.add(syncingKey);
            if (!isNewEntry) {
                emitter.onComplete();
                return;
            }

            // Does a security check to ensure that a message is never sent twice. It can occur that
            // a refreshThreads() is done in the same time as sending a message. If we just finish
            // sending the message but before it checked which message to resend in reloadThreads(),
            // we end up here twice.
            boolean shouldPush = false;
            MessageThread thread2 = getRaw(thread.id);
            if (thread2 != null) {
                for (Message m : thread2.messages) {
                    if (m.id == message.id && message.status == Message.STATUS_OUTGOING) {
                        shouldPush = true;
                        break;
                    }
                }
            }
            if (!shouldPush) {
                mSyncingMessages.remove(syncingKey);
                emitter.onComplete();
                return;
            }

            mWarmshowersService.sendMessage(thread.id, message.body)
                    .subscribe(response -> {
                        if (!response.isSuccessful()) {
                            throw new Exception(response.errorBody().toString());
                        } else if (!response.body().isSuccessful) {
                            throw new Exception("Retreived an unsuccessful response.");
                        }

                        // Sending the message was successful.

                        // We mark the temporary db message as sent.
                        message.status = Message.STATUS_SYNCED;
                        mMessageDao.saveMessage(message);

                        mSyncingMessages.remove(syncingKey);

                        if (reloadThread) {
                            // Reloads the thread. Eventually, when the call is successful, all the
                            // temporary messages that are pushed to the server are deleted.
                            reloadThread(thread.id);
                        }

                        emitter.onComplete();
                    }, throwable -> {
                        mSyncingMessages.remove(syncingKey);

                        // We only log the error and hope that it eventually succeeds.
                        Log.e(WSAndroidApplication.TAG,
                                "Could not send the message: " + throwable.toString());
                        emitter.onError(throwable);
                    });
        });
    }

    // Contains the threadId-messageId pairs that are currently syncing.
    private class SyncingKey implements Comparable<SyncingKey> {
        final int threadId;
        final int messageId;

        SyncingKey(int threadId, int messageId) {
            this.threadId = threadId;
            this.messageId = messageId;
        }

        @Override
        public int compareTo(@NonNull SyncingKey other) {
            return threadId != other.threadId
                    ? Integer.compare(threadId, other.threadId)
                    : Integer.compare(messageId, other.messageId);
        }
    }
}
