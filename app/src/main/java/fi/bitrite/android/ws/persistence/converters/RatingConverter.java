package fi.bitrite.android.ws.persistence.converters;

import fi.bitrite.android.ws.model.Feedback;

public final class RatingConverter {
    private static final int CODE_POSITIVE = 0;
    private static final int CODE_NEUTRAL = 1;
    private static final int CODE_NEGATIVE = 2;

    public static Integer ratingToInt(Feedback.Rating rating) {
        if (rating == null) {
            return null;
        }

        switch (rating) {
            case Positive: return CODE_POSITIVE;
            case Neutral: return CODE_NEUTRAL;
            case Negative: return CODE_NEGATIVE;

            default:
                throw new IllegalArgumentException("Invalid rating value: " + rating);
        }
    }

    public static Feedback.Rating intToRating(Integer rating) {
        if (rating == null) {
            return null;
        }

        switch (rating) {
            case CODE_POSITIVE: return Feedback.Rating.Positive;
            case CODE_NEUTRAL: return Feedback.Rating.Neutral;
            case CODE_NEGATIVE: return Feedback.Rating.Negative;

            default:
                throw new IllegalArgumentException("Invalid rating value: " + rating);
        }
    }
}
