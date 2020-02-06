package fr.inria.spirals.repairnator.serializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.utils.DateUtils;
import fr.inria.spirals.repairnator.utils.Utils;

public class PipelineErrorSerializer4GitRepository extends AbstractDataSerializer {
    
	public PipelineErrorSerializer4GitRepository(List<SerializerEngine> engines, ProjectInspector inspector) {
        super(engines, SerializerType.PIPELINE_ERRORS, inspector);
    }

    public void serialize() {
        List<Object> dataAsList = new ArrayList<>();
        dataAsList.add(Utils.getHostname());
        
        dataAsList.add(DateUtils.formatCompleteDate(new Date()));
        dataAsList.add(DateUtils.formatOnlyDay(new Date()));
        dataAsList.add(inspector.getGitSlug());
        dataAsList.add(inspector.getGitRepositoryId());

        for (List<String> strings : inspector.getJobStatus().getStepErrors().values()) {
            dataAsList.add(StringUtils.join(strings, "       "));
        }

        JsonObject dataAsJson = new JsonObject();
        dataAsJson.addProperty("hostname", Utils.getHostname());
        dataAsJson.addProperty("repositoryId", inspector.getGitRepositoryId());
        dataAsJson.addProperty("repositoryName", inspector.getGitSlug());
        this.addDate(dataAsJson, "computedDate", new Date());
        dataAsJson.addProperty("computedDateStr", DateUtils.formatCompleteDate(new Date()));
        dataAsJson.addProperty("computedDay", DateUtils.formatOnlyDay(new Date()));

        for (Map.Entry<String, List<String>> stringListEntry : inspector.getJobStatus().getStepErrors().entrySet()) {
            JsonArray jsonElements = new JsonArray();

            for (String message : stringListEntry.getValue()) {
                jsonElements.add(message);
            }
            dataAsJson.add(stringListEntry.getKey(), jsonElements);
        }

        List<SerializedData> serializedData = new ArrayList<>();
        serializedData.add(new SerializedData(dataAsList, dataAsJson));

        for (SerializerEngine serializerEngine : this.getEngines()) {
            serializerEngine.serialize(serializedData, this.getType());
        }

    }
}
