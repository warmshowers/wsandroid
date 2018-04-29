package fi.bitrite.android.ws.util;

public class Pushable<T> {
    public final T value;
    public final boolean isPushed;

    public Pushable(T value, boolean isPushed) {
        this.value = value;
        this.isPushed = isPushed;
    }
}

