package fi.bitrite.android.ws.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;

/**
 * Wrapper for Glide to ensure an up-to-date default `RequestOptions` being injected into each
 * request while they can change at runtime (for e.g. the data-saver mode).
 * Glide does not support this out of the box as changes to the default `RequestOptions` are not
 * passed through to `RequestManager`s that are already created.
 */
public class WSGlide {
    private static RequestOptions mRequestOptions;

    @NonNull
    public static RequestManager with(@NonNull Context context) {
        return apply(Glide.with(context));
    }

    @NonNull
    public static RequestManager with(@NonNull Activity activity) {
        return apply(Glide.with(activity));
    }

    @NonNull
    public static RequestManager with(@NonNull FragmentActivity activity) {
        return apply(Glide.with(activity));
    }

    @NonNull
    public static RequestManager with(@NonNull Fragment fragment) {
        return apply(Glide.with(fragment));
    }

    @NonNull
    public static RequestManager with(@NonNull View view) {
        return apply(Glide.with(view));
    }

    public static void setDefaultRequestOptions(@NonNull RequestOptions requestOptions) {
        mRequestOptions = requestOptions;
    }

    private static RequestManager apply(@NonNull RequestManager requestManager) {
        if(mRequestOptions != null) {
            requestManager.applyDefaultRequestOptions(mRequestOptions);
        }
        return requestManager;
    }
}
