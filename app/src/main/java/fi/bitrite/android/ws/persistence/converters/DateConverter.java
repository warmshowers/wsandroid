package fi.bitrite.android.ws.persistence.converters;

import java.util.Date;

public final class DateConverter {
    public static Long dateToLong(Date date) {
        return date == null ? null : date.getTime() / 1000; // In seconds
    }

    public static Date longToDate(Long time) {
        return time == null ? null : new Date(time * 1000);
    }
}
