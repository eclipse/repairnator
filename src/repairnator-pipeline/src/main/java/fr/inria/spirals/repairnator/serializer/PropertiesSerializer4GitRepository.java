package fr.inria.spirals.repairnator.serializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.properties.Properties;
import fr.inria.spirals.repairnator.process.inspectors.properties.PropertiesSerializerAdapter;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

public class PropertiesSerializer4GitRepository extends AbstractDataSerializer {

    public PropertiesSerializer4GitRepository(List<SerializerEngine> engines, ProjectInspector inspector) {
        super(engines, SerializerType.PROPERTIES, inspector);
    }

    @Override
    public void serialize() {
        Gson gson = new GsonBuilder().registerTypeAdapter(Properties.class, new PropertiesSerializerAdapter()).create();
        JsonObject element = (JsonObject)gson.toJsonTree(inspector.getJobStatus().getProperties());
        
        element.add("reproductionBuggyRepository", element.get("reproductionBuggyBuild"));
        element.remove("reproductionBuggyBuild");
        
        element.addProperty("runId", RepairnatorConfig.getInstance().getRunId());
        Date reproductionDateBeginning = inspector.getJobStatus().getProperties().getReproductionBuggyBuild().getReproductionDateBeginning();
        reproductionDateBeginning = reproductionDateBeginning == null ? new Date() : reproductionDateBeginning;
        this.addDate(element, "reproductionDate", reproductionDateBeginning);
        element.addProperty("buggyId", inspector.getProjectIdToBeInspected());
        if (inspector.getPatchedBuild() != null) {
            element.addProperty("patchedBuildId", inspector.getPatchedBuild().getId());
        }
        element.addProperty("status", getPrettyPrintState(inspector));

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
