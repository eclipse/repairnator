package fr.inria.spirals.repairnator.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.properties.Properties;
import fr.inria.spirals.repairnator.process.inspectors.properties.PropertiesSerializerAdapter;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by urli on 28/04/2017.
 */
public class MetricsSerializer extends AbstractDataSerializer {

    public MetricsSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.METRICS);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Properties.class, new PropertiesSerializerAdapter()).create();
        JsonObject element = (JsonObject)gson.toJsonTree(inspector.getJobStatus().getProperties());

        element.addProperty("runId", RepairnatorConfig.getInstance().getRunId());
        this.addDate(element, "reproductionDate", new Date());
        element.addProperty("buggyBuildId", inspector.getBuggyBuild().getId());
        element.addProperty("buildStatus", inspector.getBuildToBeInspected().getStatus().name());
        if (inspector.getPatchedBuild() != null) {
            element.addProperty("patchedBuildId", inspector.getPatchedBuild().getId());
        }

        element.addProperty("status",this.getPrettyPrintState(inspector));

        Map<String, Long> freeMemoryByStep = inspector.getJobStatus().getFreeMemoryByStep();
        for (Map.Entry<String, Long> stepFreeMemory : freeMemoryByStep.entrySet()) {
            element.addProperty(stepFreeMemory.getKey(), stepFreeMemory.getValue());
        }

        List<SerializedData> dataList = new ArrayList<>();

        dataList.add(new SerializedData(new ArrayList<>(), element));

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(dataList, this.getType());
        }
    }
}
