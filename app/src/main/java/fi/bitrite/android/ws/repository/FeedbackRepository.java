package fi.bitrite.android.ws.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import fi.bitrite.android.ws.api.WarmshowersAccountWebservice;
import fi.bitrite.android.ws.api.model.ApiFeedback;
import fi.bitrite.android.ws.di.AppScope;
import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.persistence.FeedbackDao;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * This repository is split into two parts. One lives in the account scope as we need access to its
 * webservice. However, feedbacks are cached in the app-level as they stay the same and are not
 * private.
 */
@AccountScope
public class FeedbackRepository {
    @Inject AppFeedbackRepository _mAppFeedbackRepository;

    @Inject UserRepository mUserRepository;
    @Inject WarmshowersAccountWebservice mWebservice;

    @Inject
    FeedbackRepository() {
    }

    public Observable<Resource<List<Feedback>>> getForRecipient(int recipientId) {
        return getForRecipient(recipientId, Repository.ShouldSaveInDb.IF_ALREADY_IN_DB);
    }
    public Observable<Resource<List<Feedback>>> getForRecipient(
            int recipientId, Repository.ShouldSaveInDb shouldSaveInDb) {

        return getAppFeedbackRepository().get(recipientId, shouldSaveInDb);
    }
    public void markAsOldForRecipient(int feedbackId) {
        getAppFeedbackRepository().markAsOld(feedbackId);
    }

    public Completable saveForRecipient(int recipientId, @NonNull List<Feedback> feedbacks) {
        return getAppFeedbackRepository().saveRx(recipientId, feedbacks);
    }

    public Completable giveFeedback(
            int recipientId, @NonNull String body, Feedback.Relation relation,
            Feedback.Rating rating, int yearWeMet, int monthWeMet) {
        return getAppFeedbackRepository().giveFeedback(recipientId, body, relation, rating,
                yearWeMet, monthWeMet);
    }

    /**
     * Sets the last webservice in the app-scoped feedback repository and returns it.
     * Always use this function instead of the direct access to the variable!
     */
    private AppFeedbackRepository getAppFeedbackRepository() {
        _mAppFeedbackRepository.mLastAccountFeedbackRepository = this;
        return _mAppFeedbackRepository;
    }

    /**
     * This is the app scope instance that is shared amongst all the account-scope instances of the
     * {@link FeedbackRepository}. We share it to only have one cache. However, we need a
     * {@link WarmshowersAccountWebservice} instance, which lives in the account scope, to fetch
     * feedbacks.
     */
    @AppScope
    static class AppFeedbackRepository extends Repository<List<Feedback>> {
        @Inject FeedbackDao mFeedbackDao;

        FeedbackRepository mLastAccountFeedbackRepository;

        @Inject
        AppFeedbackRepository() {
        }

        @Override
        void saveInDb(int recipientId, @NonNull List<Feedback> feedbacks) {
            mFeedbackDao.saveForRecipient(recipientId, feedbacks);
        }

        @Override
        Observable<LoadResult<List<Feedback>>> loadFromDb(int recipientId) {
            return Single.<LoadResult<List<Feedback>>>create(emitter -> {
                List<Feedback> feedbacks = mFeedbackDao.loadByRecipient(recipientId);
                feedbacks = feedbacks == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(feedbacks);
                emitter.onSuccess(new LoadResult<>(LoadResult.Source.DB, feedbacks));
            }).toObservable();
        }

        @Override
        Observable<LoadResult<List<Feedback>>> loadFromNetwork(int recipientId) {
            return mLastAccountFeedbackRepository.mWebservice.fetchFeedbackForRecipient(recipientId)
                    .subscribeOn(Schedulers.io())
                    .map(apiFeedbackResponse -> {
                        if (apiFeedbackResponse.isSuccessful()) {
                            // Converts the {@link ApiFeedback}s into {@link Feedback}s.
                            List<ApiFeedback> apiFeedbacks = apiFeedbackResponse.body().feedbacks;
                            List<Feedback> feedbacks = new ArrayList<>(apiFeedbacks.size());
                            for (ApiFeedback apiFeedback : apiFeedbacks) {
                                feedbacks.add(apiFeedback.toFeedback());
                            }
                            feedbacks = Collections.unmodifiableList(feedbacks);

                            return new LoadResult<>(LoadResult.Source.NETWORK, feedbacks);
                        } else {
                            throw new Error(apiFeedbackResponse.errorBody().string());
                        }
                    });
        }

        Completable giveFeedback(int recipientId, @NonNull String body, Feedback.Relation relation,
                                 Feedback.Rating rating, int yearWeMet, int monthWeMet) {
            return mLastAccountFeedbackRepository.mUserRepository.get(recipientId)
                    .subscribeOn(Schedulers.io())
                    .filter(Resource::hasData)
                    .map(userResource -> userResource.data)
                    .firstOrError()
                    .flatMap(recipient -> mLastAccountFeedbackRepository.mWebservice.giveFeedback(
                            WarmshowersAccountWebservice.FEEDBACK_NODE_TYPE, recipient.username,
                            body, relation, rating, yearWeMet, monthWeMet))
                    .flatMapCompletable(apiResponse -> {
                        if (!apiResponse.isSuccessful()) {
                            throw new Error(apiResponse.errorBody().string());
                        }

                        markAsOld(recipientId);
                        return Completable.complete();
                    });
        }
    }
}
