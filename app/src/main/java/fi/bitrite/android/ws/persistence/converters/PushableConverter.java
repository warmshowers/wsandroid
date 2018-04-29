package fi.bitrite.android.ws.persistence.converters;

import android.support.annotation.NonNull;

import fi.bitrite.android.ws.util.Pushable;

public class PushableConverter {

    public static int pushableToInt(@NonNull Pushable<Boolean> p) {
        return (p.isPushed ? 1 : 0) << 1
               | (p.value ? 1 : 0) << 0;
    }

    public static Pushable<Boolean> intToBooleanPushable(int serialized) {
        boolean isPushed = (serialized & (1 << 1)) != 0;
        Boolean value = (serialized & (1 << 0)) != 0;
        return new Pushable<>(value, isPushed);
    }
}
