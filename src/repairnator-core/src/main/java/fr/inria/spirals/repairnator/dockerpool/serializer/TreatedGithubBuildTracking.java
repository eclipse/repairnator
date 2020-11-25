package fr.inria.spirals.repairnator.dockerpool.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.utils.DateUtils;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class TreatedGithubBuildTracking extends TreatedBuildTracking {

    private String githubURL;
    private String githubCommitID;

    public TreatedGithubBuildTracking(List<SerializerEngine> engines, String runid, String url, String commitId) {
        super(engines, runid);

        this.githubURL = url;
        this.githubCommitID = commitId;
        this.status = "DETECTED";
        this.serialize();
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    protected List<Object> serializeAsList() {
        Date date = new Date();

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(runId);
        dataCol.add(DateUtils.formatCompleteDate(date));
        dataCol.add(DateUtils.formatOnlyDay(date));
        dataCol.add(Utils.getHostname());
        dataCol.add(status);
        return dataCol;
    }

    @Override
    protected JsonElement serializeAsJson() {
        Date date = new Date();

        JsonObject result = new JsonObject();
        result.addProperty("runId", runId);
        result.addProperty("githubURL", githubURL);
        result.addProperty("githubCommitID", githubCommitID);
        result.addProperty("dateReproducedBuildStr", DateUtils.formatCompleteDate(date));
        this.addDate(result, "dateReproducedBuild", date);

        result.addProperty("dayReproducedBuild", DateUtils.formatOnlyDay(date));
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("status", status);

        return result;
    }
}
