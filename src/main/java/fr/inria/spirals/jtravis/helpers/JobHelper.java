package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.JobConfig;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * Created by urli on 22/12/2016.
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
        JobConfig config = ConfigHelper.getJobConfigFromJsonElement(configJSON);
        result.setConfig(config);
        return result;
    }

    public static Job getJobFromId(int jobId) {
        String resourceUrl = TRAVIS_API_ENDPOINT+JOB_ENDPOINT+jobId;

        try {
            ResponseBody response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response.string()).getAsJsonObject();

            JsonObject jobJSON = allAnswer.getAsJsonObject("job");
            return createJobFromJsonElement(jobJSON);
        } catch (IOException e) {
            return null;
        }

    }
}
