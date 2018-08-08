package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by urli on 02/02/2017.
 */
public class InspectorTimeSerializer extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(InspectorTimeSerializer.class);

    public InspectorTimeSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.TIMES);
    }

    private List<Object> serializeAsList(ProjectInspector inspector) {
        Map<String, Integer> durations = inspector.getJobStatus().getStepsDurationsInSeconds();

        Build build = inspector.getBuggyBuild();

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId() + "");
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(Utils.formatCompleteDate(new Date()));
        dataCol.add(Utils.getHostname());
        dataCol.add(inspector.getBuildToBeInspected().getRunId());

        int totalDuration = 0;
        for (Map.Entry<String, Integer> stringIntegerEntry : durations.entrySet()) {
            dataCol.add(stringIntegerEntry.getValue());
            totalDuration += stringIntegerEntry.getValue();
        }
        dataCol.add(totalDuration);
        return dataCol;
    }

    private JsonElement serializeAsJson(ProjectInspector inspector) {
        Map<String, Integer> durations = inspector.getJobStatus().getStepsDurationsInSeconds();

        Build build = inspector.getBuggyBuild();

        JsonObject result = new JsonObject();

        result.addProperty("buildId", build.getId());
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("buildReproductionDateStr", Utils.formatCompleteDate(new Date()));
        this.addDate(result, "buildReproductionDate", new Date());

        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("runId", inspector.getBuildToBeInspected().getRunId());

        int totalDuration = 0;
        for (Map.Entry<String, Integer> stepDuration : durations.entrySet()) {
            result.addProperty(stepDuration.getKey(), stepDuration.getValue());
            totalDuration += stepDuration.getValue();
        }

        result.addProperty("totalDuration", totalDuration);

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
