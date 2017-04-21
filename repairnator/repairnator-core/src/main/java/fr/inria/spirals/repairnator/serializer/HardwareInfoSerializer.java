package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 21/04/2017.
 */
public class HardwareInfoSerializer extends ProcessSerializer {

    private String runId;
    private String buildId;

    public HardwareInfoSerializer(List<SerializerEngine> engines, String runId, String buildId) {
        super(engines, SerializerType.HARDWARE_INFO);

        this.runId = runId;
        this.buildId = buildId;
    }

    @Override
    public void serialize() {
        SerializedData data = new SerializedData(this.serializeAsList(), this.serializeAsJson());

        List<SerializedData> allData = new ArrayList<>();
        allData.add(data);

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(allData, this.getType());
        }
    }

    private JsonElement serializeAsJson() {
        JsonObject result = new JsonObject();
        result.addProperty("runId", this.runId);
        result.addProperty("buildId", this.buildId);
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("nbProcessors", Runtime.getRuntime().availableProcessors());
        result.addProperty("freeMemory", Runtime.getRuntime().freeMemory());
        result.addProperty("totalMemory", Runtime.getRuntime().totalMemory());
        return result;
    }

    private List<Object> serializeAsList() {
        List<Object> dataCol = new ArrayList<>();

        dataCol.add(this.runId);
        dataCol.add(this.buildId);
        dataCol.add(Utils.getHostname());
        dataCol.add(Runtime.getRuntime().availableProcessors());
        dataCol.add(Runtime.getRuntime().freeMemory());
        dataCol.add(Runtime.getRuntime().totalMemory());

        return dataCol;
    }
}
