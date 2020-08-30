package io.jenkins.plugins.main;

import java.util.concurrent.TimeUnit;
import java.util.Date;

public class Helpers {
	public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }
}