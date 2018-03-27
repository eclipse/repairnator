package fr.inria.spirals.repairnator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 02/02/2017.
 */
public class Utils {
    private static final SimpleDateFormat tsvCompleteDateFormat = new SimpleDateFormat("dd/MM/YY HH:mm");
    private static final SimpleDateFormat csvOnlyDayFormat = new SimpleDateFormat("dd/MM/YYYY");
    private static final SimpleDateFormat fileDateFormat = new SimpleDateFormat("YYYY-MM-dd_HHmm");
    private static final String TRAVIS_URL = "http://travis-ci.org/";
    private static final String GITHUB_URL = "https://github.com/";
    public static final char COMMA = ',';

    public static String formatCompleteDate(Date date) {
        return tsvCompleteDateFormat.format(date);
    }

    public static String formatOnlyDay(Date date) {
        return csvOnlyDayFormat.format(date);
    }

    public static String formatFilenameDate(Date date) {
        return fileDateFormat.format(date);
    }

    public static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();

        } catch (UnknownHostException e) {
            return "unknown host";
        }
    }

    public static String getTravisUrl(int buildId, String slug) {
        return TRAVIS_URL + slug + "/builds/" + buildId;
    }

    public static String getGithubRepoUrl(String slug) {
        return GITHUB_URL + slug + ".git";
    }

    public static String getDuration(Date dateBegin, Date dateEnd) {
        return DurationFormatUtils.formatDuration(dateEnd.getTime()-dateBegin.getTime(), "HH:mm", true);
    }

    public static void setLoggersLevel(Level level) {
        Logger jtravis = (Logger) LoggerFactory.getLogger("fr.inria.spirals.jtravis.helpers");
        jtravis.setLevel(level);

        Logger nopol = (Logger) LoggerFactory.getLogger("fr.inria.lille.repair.nopol");
        nopol.setLevel(level);

        Logger repairnator = (Logger) LoggerFactory.getLogger("fr.inria.spirals.repairnator");
        repairnator.setLevel(level);

        Logger jgit = (Logger) LoggerFactory.getLogger("org.eclipse.jgit");
        jgit.setLevel(Level.WARN);
    }

    public static String getValue(List<Object> value, int index) {
        if (index < value.size()) {
            return value.get(index).toString();
        } else {
            return null;
        }
    }

    public static Date getLastTimeFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getMaximum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));
        return calendar.getTime();
    }

}
