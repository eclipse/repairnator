package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PipelineErrorSerializer extends AbstractDataSerializer {
    public PipelineErrorSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.PIPELINE_ERRORS);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        List<Object> dataAsList = new ArrayList<>();
        dataAsList.add(Utils.getHostname());
        dataAsList.add(inspector.getBuildToBeInspected().getRunId());
        dataAsList.add(Utils.formatCompleteDate(new Date()));
        dataAsList.add(Utils.formatOnlyDay(new Date()));
        dataAsList.add(inspector.getRepoSlug());
        dataAsList.add(inspector.getBuggyBuild().getId());

        for (List<String> strings : inspector.getJobStatus().getStepErrors().values()) {
            dataAsList.add(StringUtils.join(strings, "       "));
        }

        JsonObject dataAsJson = new JsonObject();
        dataAsJson.addProperty("hostname", Utils.getHostname());
        dataAsJson.addProperty("runId", inspector.getBuildToBeInspected().getRunId());
        dataAsJson.addProperty("buildId", inspector.getBuggyBuild().getId());
        dataAsJson.addProperty("repositoryName", inspector.getRepoSlug());
        this.addDate(dataAsJson, "computedDate", new Date());
        dataAsJson.addProperty("computedDateStr", Utils.formatCompleteDate(new Date()));
        dataAsJson.addProperty("computedDay", Utils.formatOnlyDay(new Date()));

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
