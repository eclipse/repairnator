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
public class PropertiesSerializer extends AbstractDataSerializer {

    public PropertiesSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.PROPERTIES);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Properties.class, new PropertiesSerializerAdapter()).create();
        JsonObject element = (JsonObject)gson.toJsonTree(inspector.getJobStatus().getProperties());

        element.addProperty("runId", RepairnatorConfig.getInstance().getRunId());
        Date reproductionDateBeginning = inspector.getJobStatus().getProperties().getReproductionBuggyBuild().getReproductionDateBeginning();
        reproductionDateBeginning = reproductionDateBeginning == null ? new Date() : reproductionDateBeginning;
        this.addDate(element, "reproductionDate", reproductionDateBeginning);
        element.addProperty("buildStatus", inspector.getBuildToBeInspected().getStatus().name());
        element.addProperty("buggyBuildId", inspector.getBuggyBuild().getId());
        if (inspector.getPatchedBuild() != null) {
            element.addProperty("patchedBuildId", inspector.getPatchedBuild().getId());
        }
        element.addProperty("status",this.getPrettyPrintState(inspector));

        JsonObject elementFreeMemory = new JsonObject();
        Map<String, Long> freeMemoryByStep = inspector.getJobStatus().getFreeMemoryByStep();
        for (Map.Entry<String, Long> stepFreeMemory : freeMemoryByStep.entrySet()) {
            elementFreeMemory.addProperty(stepFreeMemory.getKey(), stepFreeMemory.getValue());
        }
        element.add("freeMemoryByStep", elementFreeMemory);

        List<SerializedData> dataList = new ArrayList<>();

        dataList.add(new SerializedData(new ArrayList<>(), element));

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(dataList, this.getType());
        }
    }
}
