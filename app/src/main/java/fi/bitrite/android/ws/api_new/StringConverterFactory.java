package fi.bitrite.android.ws.api_new;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import fi.bitrite.android.ws.api_new.typeadapter.RatingTypeAdapter;
import fi.bitrite.android.ws.api_new.typeadapter.RelationTypeAdapter;
import fi.bitrite.android.ws.model.Feedback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

final class StringConverterFactory extends Converter.Factory {
    private static final MediaType MEDIA_TYPE = MediaType.parse("text/plain");

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {

        if (String.class.equals(type)) {
            return (Converter<ResponseBody, String>) ResponseBody::string;
        }

        return null;
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(
            Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations,
            Retrofit retrofit) {

        if (String.class.equals(type)) {
            return (Converter<String, RequestBody>) value -> RequestBody.create(MEDIA_TYPE, value);
        }

        return null;
    }

    /**
     * Converter for retrofit2 strings. This is used for e.g. serialization of fields.
     */
    @Override
    public Converter<?, String> stringConverter(final Type type, final Annotation[] annotations,
                                                final Retrofit retrofit) {
        if (Feedback.Rating.class.equals(type)) {
            return (Converter<Feedback.Rating, String>) RatingTypeAdapter::serialize;
        } else if (Feedback.Relation.class.equals(type)) {
            return (Converter<Feedback.Relation, String>) RelationTypeAdapter::serialize;
        }

        return null;
    }
}
