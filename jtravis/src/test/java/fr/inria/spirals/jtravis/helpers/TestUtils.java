package fr.inria.spirals.jtravis.helpers;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by urli on 22/12/2016.
 */
public class TestUtils {

    public static Date getDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar date = Calendar.getInstance();
        date.setTimeZone(TimeZone.getTimeZone("GMT+00"));
        date.setTimeInMillis(0);
        date.set(year, month-1, day, hour, minute, second);
        return date.getTime();
    }
}
