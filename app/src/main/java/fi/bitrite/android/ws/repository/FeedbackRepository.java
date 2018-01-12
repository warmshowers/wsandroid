package fi.bitrite.android.ws.repository;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.api_new.WarmshowersService;
import fi.bitrite.android.ws.api_new.model.ApiFeedback;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.persistence.FeedbackDao;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class FeedbackRepository extends Repository<List<Feedback>> {
    @Inject FeedbackDao mFeedbackDao;
    @Inject WarmshowersService mWarmshowersService;

    @Inject
    FeedbackRepository() {
    }

    public Observable<Resource<List<Feedback>>> getForRecipient(int recipientId) {
        return getForRecipient(recipientId, ShouldSaveInDb.IF_ALREADY_IN_DB);
    }
    public Observable<Resource<List<Feedback>>> getForRecipient(
            int recipientId, ShouldSaveInDb shouldSaveInDb) {

        return super.get(recipientId, shouldSaveInDb);
    }

    public void saveForRecipient(int recipientId, @NonNull List<Feedback> feedbacks) {
        super.save(recipientId, feedbacks);
    }

    @Override
    void saveInDb(int recipientId, @NonNull List<Feedback> feedbacks) {
        mFeedbackDao.saveForRecipient(recipientId, feedbacks);
    }

    @Override
    Observable<LoadResult<List<Feedback>>> loadFromDb(int recipientId) {
        return Single.<LoadResult<List<Feedback>>>create(emitter -> {
            List<Feedback> feedbacks = mFeedbackDao.loadByRecipient(recipientId);
            if (feedbacks == null) {
                feedbacks = new ArrayList<>();
            }
            emitter.onSuccess(new LoadResult<>(LoadResult.Source.DB, feedbacks));
        }).toObservable();
    }

    @Override
    Observable<LoadResult<List<Feedback>>> loadFromNetwork(int recipientId) {
        return mWarmshowersService.fetchFeedbackForRecipient(recipientId)
                .subscribeOn(Schedulers.io())
                .map(apiFeedbackResponse -> {
                    if (apiFeedbackResponse.isSuccessful()) {
                        // Converts the {@link ApiFeedback}s into {@link Feedback}s.
                        List<ApiFeedback> apiFeedbacks = apiFeedbackResponse.body().feedbacks;
                        List<Feedback> feedbacks = new ArrayList<>(apiFeedbacks.size());
                        for (ApiFeedback apiFeedback : apiFeedbacks) {
                            feedbacks.add(apiFeedback.toFeedback());
                        }

                        return new LoadResult<>(LoadResult.Source.NETWORK, feedbacks);
                    } else {
                        throw new Error(apiFeedbackResponse.errorBody().toString());
                    }
                });
    }
}
