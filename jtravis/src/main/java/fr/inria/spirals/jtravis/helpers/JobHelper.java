package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Config;
import fr.inria.spirals.jtravis.entities.Job;
import okhttp3.ResponseBody;

import java.io.IOException;

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

    private static JobHelper getInstance() {
        if (instance == null) {
            instance = new JobHelper();
        }
        return instance;
    }

    public static Job createJobFromJsonElement(JsonObject jobJson) {
        Job result = createGson().fromJson(jobJson, Job.class);

        JsonElement configJSON = jobJson.getAsJsonObject("config");
        Config config = ConfigHelper.getConfigFromJsonElement(configJSON);
        result.setConfig(config);
        return result;
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
}
