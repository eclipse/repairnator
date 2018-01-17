package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import fr.inria.spirals.jtravis.entities.Config;
import fr.inria.spirals.jtravis.entities.Job;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The helper to deal with job objects
 *
 * @author Simon Urli
 */
public class JobHelper extends AbstractHelper {
    public static final String JOB_ENDPOINT = "jobs/";

    private static JobHelper instance;

    private JobHelper() {
        super();
    }

    protected static JobHelper getInstance() {
        if (instance == null) {
            instance = new JobHelper();
        }
        return instance;
    }

    public static Job createJobFromJsonElement(JsonObject jobJson) {
        try {
            Job result = createGson().fromJson(jobJson, Job.class);

            if (jobJson.has("config")) {
                JsonElement configJSON = jobJson.getAsJsonObject("config");
                Config config = ConfigHelper.getConfigFromJsonElement(configJSON);
                result.setConfig(config);
            }
            return result;
        } catch (JsonSyntaxException jsonSyntaxException) {
            getInstance().getLogger().error("Error while creating job node: ", jsonSyntaxException);
            return null;
        }
    }

    public static Job getJobFromId(int jobId) {
        String resourceUrl = getInstance().getEndpoint()+JOB_ENDPOINT+jobId;

        try {
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();

            JsonObject jobJSON = allAnswer.getAsJsonObject("job");
            return createJobFromJsonElement(jobJSON);
        } catch (IOException e) {
            getInstance().getLogger().warn("Error when getting job id "+jobId+" : "+e.getMessage());
            return null;
        }
    }

    public static List<Job> getJobList() {
        String resourceUrl = getInstance().getEndpoint()+JOB_ENDPOINT;

        try {
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonArray allAnswers = parser.parse(response).getAsJsonObject().getAsJsonArray("jobs");
            List<Job> results = new ArrayList<>();
            for (JsonElement jobJson : allAnswers) {
                Job job = createJobFromJsonElement((JsonObject) jobJson);
                if (job != null) {
                    results.add(job);
                }
            }
            return results;
        } catch (IOException e) {
            getInstance().getLogger().warn("Error while getting list of jobs : "+e.getMessage());
        }
        return null;
    }
}
