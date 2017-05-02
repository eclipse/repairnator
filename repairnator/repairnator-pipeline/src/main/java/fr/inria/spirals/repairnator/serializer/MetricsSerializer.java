package fr.inria.spirals.repairnator.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.MetricsSerializerAdapter;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 28/04/2017.
 */
public class MetricsSerializer extends AbstractDataSerializer {

    public MetricsSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.METRICS);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Metrics.class, new MetricsSerializerAdapter()).create();
        JsonObject element = (JsonObject)gson.toJsonTree(inspector.getJobStatus().getMetrics());

        element.addProperty("runId", RepairnatorConfig.getInstance().getRunId());
        this.addDate(element, "reproductionDate", new Date());
        element.addProperty("buggyBuildId", inspector.getBuggyBuild().getId());
        element.addProperty("buildStatus", inspector.getBuildToBeInspected().getStatus().name());
        if (inspector.getPatchedBuild() != null) {
            element.addProperty("patchedBuildId", inspector.getPatchedBuild().getId());
        }

        element.addProperty("status",this.getPrettyPrintState(inspector));

        List<SerializedData> dataList = new ArrayList<>();

        dataList.add(new SerializedData(new ArrayList<>(), element));

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(dataList, this.getType());
        }
    }
}
