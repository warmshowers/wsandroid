package fi.bitrite.android.ws.util;

public final class MaybeNull<T> {
    public final T data;

    public MaybeNull() {
        this(null);
    }
    public MaybeNull(T data) {
        this.data = data;
    }

    public boolean isNonNull() {
        return !isNull();
    }
    public boolean isNull() {
        return data == null;
    }
}

