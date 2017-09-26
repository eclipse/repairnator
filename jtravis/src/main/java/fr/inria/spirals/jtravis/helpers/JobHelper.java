package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Config;
import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Repository;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * The helper to deal with job objects
 *
 * @author Simon Urli
 */
public class JobHelper extends AbstractHelper {
    public static final String JOB_ENDPOINT = "jobs/";
    public static final String JOB_ENDPOINTV3 = "job/";

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
        Job result = createGson().fromJson(jobJson, Job.class);

        if(!Version.getVersionV3()) {
        JsonElement configJSON = jobJson.getAsJsonObject("config");
        Config config = ConfigHelper.getConfigFromJsonElement(configJSON);
        result.setConfig(config);
        }

        return result;
    }

    public static Job getJobFromId(int jobId) {
        String resourceUrl = "";
        if(Version.getVersionV3())
            resourceUrl = getInstance().getEndpoint()+JOB_ENDPOINTV3+jobId;
        else
            resourceUrl = getInstance().getEndpoint()+JOB_ENDPOINT+jobId;

        try {
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();
            if(Version.getVersionV3()) {
                Job job = createJobFromJsonElement(allAnswer);
                JsonObject repoJSON = allAnswer.getAsJsonObject("repository");
                job.setRepositoryId(repoJSON.get("id").getAsInt());
                JsonObject commitJSON = allAnswer.getAsJsonObject("commit");
                job.setCommitId(commitJSON.get("id").getAsInt());
                JsonObject buildJSON = allAnswer.getAsJsonObject("build");
                job.setBuildId(buildJSON.get("id").getAsInt());
                return job;
            }
            else {
                JsonObject jobJSON = allAnswer.getAsJsonObject("job");
                return createJobFromJsonElement(jobJSON);
            }
        } catch (IOException e) {
            getInstance().getLogger().warn("Error when getting job id "+jobId+" : "+e.getMessage());
            return null;
        }

    }
}
