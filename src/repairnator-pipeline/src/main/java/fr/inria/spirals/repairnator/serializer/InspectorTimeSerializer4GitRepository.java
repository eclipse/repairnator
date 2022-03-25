package fr.inria.spirals.repairnator.serializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.utils.DateUtils;
import fr.inria.spirals.repairnator.utils.Utils;

public class InspectorTimeSerializer4GitRepository extends AbstractDataSerializer {

	public InspectorTimeSerializer4GitRepository(List<SerializerEngine> engines, ProjectInspector inspector) {
        super(engines, SerializerType.TIMES, inspector);
    }

//    private List<Object> serializeAsList(ProjectInspector inspector) {
//        Map<String, Integer> durations = inspector.getJobStatus().getStepsDurationsInSeconds();
//        List<Object> dataCol = new ArrayList<Object>();
//
//        dataCol.add(inspector.getProjectIdToBeInspected());
//        dataCol.add(inspector.getGitSlug());
//        dataCol.add(DateUtils.formatCompleteDate(new Date()));
//        dataCol.add(Utils.getHostname());
//
//        int totalDuration = 0;
//        for (Map.Entry<String, Integer> stringIntegerEntry : durations.entrySet()) {
//            dataCol.add(stringIntegerEntry.getValue());
//            totalDuration += stringIntegerEntry.getValue();
//        }
//        dataCol.add(totalDuration);
//        return dataCol;
//    }

    private JsonElement serializeAsJson(ProjectInspector inspector) {
        Map<String, Integer> durations = inspector.getJobStatus().getStepsDurationsInSeconds();
        JsonObject result = new JsonObject();

        result.addProperty("repositoryId", inspector.getProjectIdToBeInspected());
        result.addProperty("repositoryName", inspector.getGitSlug());
        result.addProperty("reproductionDateStr", DateUtils.formatCompleteDate(new Date()));
        this.addDate(result, "reproductionDate", new Date());

        result.addProperty("hostname", Utils.getHostname());

        int totalDuration = 0;
        for (Map.Entry<String, Integer> stepDuration : durations.entrySet()) {
            result.addProperty(stepDuration.getKey(), stepDuration.getValue());
            totalDuration += stepDuration.getValue();
        }

        result.addProperty("totalDuration", totalDuration);

        return result;
    }


    @Override
    public void serialize() {
        SerializedData data = new SerializedData(ProjectInspector.serializeAsList(inspector), this.serializeAsJson(inspector));

        List<SerializedData> allData = new ArrayList<>();
        allData.add(data);

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(allData, this.getType());
        }
    }

}
