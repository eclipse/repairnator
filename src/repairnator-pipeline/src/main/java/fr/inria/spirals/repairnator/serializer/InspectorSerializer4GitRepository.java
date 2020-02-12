package fr.inria.spirals.repairnator.serializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.utils.DateUtils;
import fr.inria.spirals.repairnator.utils.Utils;

public class InspectorSerializer4GitRepository extends AbstractDataSerializer {
	private Logger logger = LoggerFactory.getLogger(InspectorSerializer.class);

    public InspectorSerializer4GitRepository(List<SerializerEngine> engines, ProjectInspector inspector) {
        super(engines, SerializerType.INSPECTOR, inspector);
    }

    private List<Object> serializeAsList(ProjectInspector inspector) {
        JobStatus jobStatus = inspector.getJobStatus();
        String state = getPrettyPrintState(inspector);
        String realState = StringUtils.join(jobStatus.getStepStatuses(), " -> ");
        String typeOfFailures = StringUtils.join(jobStatus.getFailureNames(), ",") + "";

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(inspector.getGitRepositoryId());
        dataCol.add(inspector.getGitSlug());
        dataCol.add(state);
        dataCol.add(realState);
        dataCol.add(Utils.getHostname());
        dataCol.add(DateUtils.formatCompleteDate(new Date()));
        dataCol.add(typeOfFailures);
        
        return dataCol;
    }

    private JsonElement serializeAsJson(ProjectInspector inspector) {
        JobStatus jobStatus = inspector.getJobStatus();
        String state = getPrettyPrintState(inspector);
        String realState = StringUtils.join(jobStatus.getStepStatuses(), " -> ");
        String typeOfFailures = StringUtils.join(jobStatus.getFailureNames(), ",");

        JsonObject result = new JsonObject();
        result.addProperty("repositoryId", inspector.getGitRepositoryId());
        result.addProperty("repositoryName", inspector.getGitSlug());
        result.addProperty("status", state);
        result.addProperty("realStatus", realState);
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("repositoryReproductionDateStr", DateUtils.formatCompleteDate(new Date()));
        this.addDate(result, "repositoryReproductionDate", new Date());
        result.addProperty("typeOfFailures", typeOfFailures);
        result.addProperty("branchURL", jobStatus.getGitBranchUrl());

        return result;
    }

    @Override
    public void serialize() {
        SerializedData data = new SerializedData(this.serializeAsList(inspector), this.serializeAsJson(inspector));

        List<SerializedData> allData = new ArrayList<>();
        allData.add(data);

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(allData, this.getType());
        }
    }
}
