package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by fermadeiral.
 */
public class InspectorSerializer4Bears extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(InspectorSerializer4Bears.class);

    public InspectorSerializer4Bears(List<SerializerEngine> engines) {
        super(engines, SerializerType.INSPECTOR4BEARS);
    }

    private List<Object> serializeAsList(ProjectInspector inspector) {
        JobStatus jobStatus = inspector.getJobStatus();
        BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();
        Build build = inspector.getBuild();

        Build previousBuild = inspector.getPreviousBuild();
        int previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

        String state = this.getPrettyPrintState(jobStatus);

        String realState = (jobStatus.getState() != null) ? jobStatus.getState().name() : "null";

        String typeOfFailures = StringUtils.join(jobStatus.getFailureNames(), ",")+"";
        String previousBuildSlug = (previousBuild != null) ? previousBuild.getRepository().getSlug() : "";

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId() + "");
        dataCol.add(previousBuildId + "");
        dataCol.add(inspector.getBuildToBeInspected().getStatus().toString());
        dataCol.add(state);
        dataCol.add(realState);
        dataCol.add(typeOfFailures);
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(build.getPullRequestNumber() + "");
        dataCol.add(Utils.formatCompleteDate(build.getFinishedAt()));
        dataCol.add(Utils.formatOnlyDay(build.getFinishedAt()));
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.formatCompleteDate(new Date()));
        dataCol.add(Utils.getTravisUrl(build.getId(), build.getRepository().getSlug()));
        dataCol.add(Utils.getTravisUrl(previousBuildId, previousBuildSlug));
        if (jobStatus.getState() == ProjectState.FIXERBUILDCASE1 || jobStatus.getState() == ProjectState.FIXERBUILDCASE2) {
            String committerEmail = (build.getCommit().getCommitterEmail() != null) ? build.getCommit().getCommitterEmail() : "-";
            dataCol.add(committerEmail);
        }
        dataCol.add(buildToBeInspected.getRunId());

        return dataCol;
    }

    private JsonElement serializeAsJson(ProjectInspector inspector) {
        JobStatus jobStatus = inspector.getJobStatus();
        BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();
        Build build = inspector.getBuild();

        Build previousBuild = inspector.getPreviousBuild();
        int previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

        String state = this.getPrettyPrintState(jobStatus);

        String realState = (jobStatus.getState() != null) ? jobStatus.getState().name() : "null";

        String typeOfFailures = StringUtils.join(jobStatus.getFailureNames(), ",");
        String previousBuildSlug = (previousBuild != null) ? previousBuild.getRepository().getSlug() : "";
        String committerEmail = (build.getCommit().getCommitterEmail() != null) ? build.getCommit().getCommitterEmail() : "-";

        JsonObject result = new JsonObject();

        result.addProperty("buildId", build.getId());
        result.addProperty("previousBuildId", previousBuildId);
        result.addProperty("scannedBuildStatus", buildToBeInspected.getStatus().name());
        result.addProperty("status", state);
        result.addProperty("realStatus", realState);
        result.addProperty("typeOfFailures", typeOfFailures);
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("prNumber", build.getPullRequestNumber());
        result.addProperty("buildFinishedDate", Utils.formatCompleteDate(build.getFinishedAt()));
        result.addProperty("buildFinishedDay", Utils.formatOnlyDay(build.getFinishedAt()));
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("buildReproductionDate", Utils.formatCompleteDate(new Date()));
        result.addProperty("buildTravisUrl", Utils.getTravisUrl(build.getId(), build.getRepository().getSlug()));
        result.addProperty("previousBuildTravisUrl", Utils.getTravisUrl(previousBuildId, previousBuildSlug));
        result.addProperty("committerEmail", committerEmail);
        result.addProperty("runId", buildToBeInspected.getRunId());

        return result;
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        SerializedData data = new SerializedData(this.serializeAsList(inspector), this.serializeAsJson(inspector));

        List<SerializedData> allData = new ArrayList<>();
        allData.add(data);

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(allData, this.getType());
        }
    }
}
