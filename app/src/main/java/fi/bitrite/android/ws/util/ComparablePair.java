package fi.bitrite.android.ws.util;

import android.support.annotation.NonNull;
import android.util.Pair;

public class ComparablePair<F extends Comparable<? super F>, S extends Comparable<? super S>>
        extends Pair<F, S> implements Comparable<ComparablePair<F, S>> {

    public ComparablePair(F first, S second) {
        super(first, second);
    }

    @Override
    public int compareTo(@NonNull ComparablePair<F, S> other) {
        int f = ObjectUtils.compare(first, other.first);
        return f != 0 ? f : ObjectUtils.compare(second, other.second);
    }
}
