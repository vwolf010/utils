package nl.v4you.date;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DateUtils {
    private static final String DATE_ZULU_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String DATE_ZULU_FORMAT_MSEC = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static String getZuluDate(long epochMilliSeconds) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_ZULU_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(epochMilliSeconds);
    }

    public static String getZuluDateMsec(long epochMilliSeconds) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_ZULU_FORMAT_MSEC);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(epochMilliSeconds);
    }
}