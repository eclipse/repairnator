package fr.inria.spirals.repairnator.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.properties.Metrics4Bears;
import fr.inria.spirals.repairnator.process.inspectors.properties.MetricsSerializerAdapter4Bears;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.List;

public class MetricsSerializer4Bears extends AbstractDataSerializer {

    public MetricsSerializer4Bears(List<SerializerEngine> engines) {
        super(engines, SerializerType.METRICS4BEARS);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Metrics4Bears.class, new MetricsSerializerAdapter4Bears()).create();
        JsonObject element = (JsonObject)gson.toJsonTree(inspector.getJobStatus().getMetrics4Bears());

        element.addProperty("runId", RepairnatorConfig.getInstance().getRunId());

        List<SerializedData> dataList = new ArrayList<>();

        dataList.add(new SerializedData(new ArrayList<>(), element));

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(dataList, this.getType());
        }
    }
}
