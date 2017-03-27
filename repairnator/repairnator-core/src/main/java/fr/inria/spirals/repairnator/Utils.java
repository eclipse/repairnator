package fr.inria.spirals.repairnator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by urli on 02/02/2017.
 */
public class Utils {
    public static final String[] ENVIRONMENT_VARIABLES = new String[] { "M2_HOME", "GITHUB_OAUTH", "GITHUB_LOGIN" };
    private static final SimpleDateFormat tsvCompleteDateFormat = new SimpleDateFormat("dd/MM/YY HH:mm");
    private static final SimpleDateFormat csvOnlyDayFormat = new SimpleDateFormat("dd/MM/YYYY");
    private static final SimpleDateFormat fileDateFormat = new SimpleDateFormat("YYYY-MM-dd_HHmm");
    private static final String TRAVIS_URL = "http://travis-ci.org/";

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
}
