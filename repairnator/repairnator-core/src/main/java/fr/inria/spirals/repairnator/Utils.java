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

    public static final String GITHUB_OAUTH = "GITHUB_OAUTH";
    public static final String M2_HOME = "M2_HOME";

    private static final SimpleDateFormat tsvCompleteDateFormat = new SimpleDateFormat("dd/MM/YY HH:mm");
    private static final SimpleDateFormat csvOnlyDayFormat = new SimpleDateFormat("dd/MM/YYYY");
    private static final SimpleDateFormat fileDateFormat = new SimpleDateFormat("YYYY-MM-dd_HHmm");

    private static final String TRAVIS_URL = "http://travis-ci.org/";
    public static final String TRAVIS_FILE = ".travis.yml";

    public static final String GITHUB_URL = "https://github.com/";
    public static final String REMOTE_REPO_EXT = ".git";
    public static final String GITHUB_USER_NAME_PATTERN = "[a-zA-Z0-9](?:[a-zA-Z0-9]|[-](?=[a-zA-Z0-9]))*";
    public static final String GITHUB_REPO_NAME_PATTERN = "[a-zA-Z0-9-_.]+(?<!\\.git)";
    private static final String GITHUB_REPO_URL_PATTERN = GITHUB_URL + GITHUB_USER_NAME_PATTERN + "/" + GITHUB_REPO_NAME_PATTERN;

    public static final String POM_FILE = "pom.xml";

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

    public static String getTravisUrl(long buildId, String slug) {
        return TRAVIS_URL + slug + "/builds/" + String.valueOf(buildId);
    }

    public static String getSimpleGithubRepoUrl(String slug) {
        return GITHUB_URL + slug;
    }

    public static String getCompleteGithubRepoUrl(String slug) {
        return GITHUB_URL + slug + REMOTE_REPO_EXT;
    }

    public static String getCommitUrl(String commitId, String repoSlug) {
        return "http://github.com/"+repoSlug+"/commit/"+commitId;
    }

    public static String getBranchUrl(String branchName, String repoSlug) {
        return "http://github.com/"+repoSlug+"/tree/"+branchName;
    }

    public static boolean matchesGithubRepoUrl(String repoUrl) {
        return repoUrl.matches(GITHUB_REPO_URL_PATTERN);
    }

    public static String getDuration(Date dateBegin, Date dateEnd) {
        return DurationFormatUtils.formatDuration(dateEnd.getTime()-dateBegin.getTime(), "HH:mm", true);
    }

    public static void setLoggersLevel(Level level) {
        Logger allinria = (Logger) LoggerFactory.getLogger("fr.inria");
        allinria.setLevel(level);

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
