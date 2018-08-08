package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
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
 * Created by urli on 02/02/2017.
 */
public class InspectorSerializer extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(InspectorSerializer.class);

    public InspectorSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.INSPECTOR);
    }

    private List<Object> serializeAsList(ProjectInspector inspector) {
        JobStatus jobStatus = inspector.getJobStatus();
        BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();
        Build build = inspector.getBuggyBuild();

        String state = getPrettyPrintState(inspector);

        String realState = StringUtils.join(jobStatus.getStepStatuses(), " -> ");
        String typeOfFailures = StringUtils.join(jobStatus.getFailureNames(), ",")+"";

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId() + "");
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(state);
        dataCol.add(build.getPullRequestNumber() + "");
        dataCol.add(Utils.formatCompleteDate(build.getFinishedAt()));
        dataCol.add(Utils.formatOnlyDay(build.getFinishedAt()));
        dataCol.add(realState);
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.formatCompleteDate(new Date()));
        dataCol.add(Utils.getTravisUrl(build.getId(), build.getRepository().getSlug()));
        dataCol.add(typeOfFailures);
        dataCol.add(buildToBeInspected.getRunId());

        return dataCol;
    }

    private JsonElement serializeAsJson(ProjectInspector inspector) {
        JobStatus jobStatus = inspector.getJobStatus();
        BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();
        Build build = inspector.getBuggyBuild();

        String state = getPrettyPrintState(inspector);

        String realState = StringUtils.join(jobStatus.getStepStatuses(), " -> ");
        String typeOfFailures = StringUtils.join(jobStatus.getFailureNames(), ",");

        JsonObject result = new JsonObject();
        result.addProperty("buildId", build.getId());
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("status", state);
        result.addProperty("prNumber", build.getPullRequestNumber());
        result.addProperty("buildFinishedDateStr", Utils.formatCompleteDate(build.getFinishedAt()));
        this.addDate(result, "buildFinishedDate", build.getFinishedAt());

        result.addProperty("buildFinishedDay", Utils.formatOnlyDay(build.getFinishedAt()));
        result.addProperty("realStatus", realState);
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("buildReproductionDateStr", Utils.formatCompleteDate(new Date()));
        this.addDate(result, "buildReproductionDate", new Date());

        result.addProperty("travisURL", Utils.getTravisUrl(build.getId(), build.getRepository().getSlug()));
        result.addProperty("typeOfFailures", typeOfFailures);
        result.addProperty("runId", buildToBeInspected.getRunId());
        result.addProperty("branchURL", jobStatus.getGitBranchUrl());

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
