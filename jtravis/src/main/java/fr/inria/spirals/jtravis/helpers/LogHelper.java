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

    protected static LogHelper getInstance() {
        if (instance == null) {
            instance = new LogHelper();
        }
        return instance;
    }

    public static Log getLogFromJob(Job job) {
        if (job.getId() != 0) {
            if (job.getBuildStatus() == BuildStatus.FAILED || job.getBuildStatus() == BuildStatus.PASSED || job.getBuildStatus() == BuildStatus.ERRORED) {
                String logJobUrl = getInstance().getEndpoint()+JobHelper.JOB_ENDPOINT+job.getId()+LOG_JOB_ENDPOINT;
                try {
                    String body = getInstance().rawGet(logJobUrl);
                    return new Log(job.getId(), body);
                } catch (IOException e) {
                    getInstance().getLogger().warn("Error when getting log from job id "+job.getId()+" ",e);
                }
            } else {
                getInstance().getLogger().warn("Error when getting log from job id "+job.getId()+" : build status is neither failed or passed: "+job.getBuildStatus().name());
            }
        }
        return null;
    }
}
