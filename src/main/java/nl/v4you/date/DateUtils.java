package nl.v4you.date;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DateUtils {
    private static SimpleDateFormat zuluFormat;
    private static SimpleDateFormat zuluFormatMsec;

    static {
        zuluFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        zuluFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        zuluFormatMsec = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        zuluFormatMsec.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String getZuluDate(long epochMilliSeconds) {
        return zuluFormat.format(epochMilliSeconds);
    }

    public static String getZuluDateMsec(long epochMilliSeconds) {
        return zuluFormatMsec.format(epochMilliSeconds);
    }
}
