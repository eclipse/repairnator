package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Log;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * The helper to deal with log objects
 *
 * @author Simon Urli
 */
public class LogHelper extends AbstractHelper {
    public final static String LOG_ENDPOINT = "logs/";
    public final static String LOG_JOB_ENDPOINT = "/log";

    private static LogHelper instance;

    private LogHelper() {
        super();
    }

    private static LogHelper getInstance() {
        if (instance == null) {
            instance = new LogHelper();
        }
        return instance;
    }

    public static Log getLogFromId(int logId) {
        try {
            String response = getInstance().get(TRAVIS_API_ENDPOINT+LOG_ENDPOINT+logId);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();
            JsonObject logJson = allAnswer.getAsJsonObject("log");
            return createGson().fromJson(logJson, Log.class);
        } catch (IOException e) {
            getInstance().getLogger().warn("Error when getting log id "+logId+" : "+e.getMessage());
            return null;
        }
    }

    public static String getRawLogFromEmptyLog(Log log) {
        if (log.getJobId() != 0) {
            Job job = JobHelper.getJobFromId(log.getJobId());
            if (job.getBuildStatus() == BuildStatus.FAILED || job.getBuildStatus() == BuildStatus.PASSED) {
                String logJobUrl = TRAVIS_API_ENDPOINT+JobHelper.JOB_ENDPOINT+log.getJobId()+LOG_JOB_ENDPOINT;
                try {
                    return getInstance().rawGet(logJobUrl);
                } catch (IOException e) {
                    getInstance().getLogger().warn("Error when getting raw log "+log.getId()+" : "+e.getMessage());
                }
            }
        }
        return null;
    }
}
