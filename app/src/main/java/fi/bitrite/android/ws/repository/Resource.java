package fi.bitrite.android.ws.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
public class Resource<T> {

    /**
     * Status of a resource that is provided to the UI.
     *
     * These are usually created by the Repository classes where they return
     * {@code LiveData<Resource<T>>} to pass back the latest data to the UI with its fetch status.
     */
    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    public final static int UNKNOWN_ERROR = 0;

    @NonNull
    public final Status status;

    @Nullable
    public final Throwable error;

    @Nullable
    public final T data;

    public Resource(@NonNull Status status, @Nullable T data, @Nullable Throwable error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> error(Throwable error, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, error);
    }

    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    public boolean isLoading() {
        return status == Status.LOADING;
    }
    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean hasData() {
        return data != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Resource<?> resource = (Resource<?>) o;

        if (status != resource.status) {
            return false;
        }
        if (error != null ? !error.equals(resource.error) : resource.error != null) {
            return false;
        }
        return data != null ? data.equals(resource.data) : resource.data == null;
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + (error != null ? error.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Resource{" +
               "status=" + status +
               ", error='" + error + '\'' +
               ", data=" + data +
               '}';
    }
}