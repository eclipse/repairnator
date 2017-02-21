package fr.inria.spirals.repairnator.serializer;

import org.apache.commons.lang.time.DurationFormatUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by urli on 02/02/2017.
 */
public class SerializerUtils {

    private static final SimpleDateFormat tsvCompleteDateFormat = new SimpleDateFormat("dd/MM/YY HH:mm");
    private static final SimpleDateFormat csvOnlyDayFormat = new SimpleDateFormat("dd/MM/YYYY");

    public static String formatCompleteDate(Date date) {
        return tsvCompleteDateFormat.format(date);
    }

    public static String formatOnlyDay(Date date) {
        return csvOnlyDayFormat.format(date);
    }

    public static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();

        } catch (UnknownHostException e) {
            return "unknown host";
        }
    }

    public static String getDuration(Date dateBegin, Date dateEnd) {
        return DurationFormatUtils.formatDuration(dateEnd.getTime()-dateBegin.getTime(), "HH:mm", true);
    }
}
