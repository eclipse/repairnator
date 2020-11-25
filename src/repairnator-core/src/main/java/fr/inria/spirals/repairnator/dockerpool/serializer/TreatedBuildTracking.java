package fr.inria.spirals.repairnator.dockerpool.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.serializer.SerializerImpl;
import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.List;

public abstract class TreatedBuildTracking extends SerializerImpl {
    protected String runId;
    protected String containerId;
    protected String status;

    public TreatedBuildTracking(List<SerializerEngine> engines, String runId) {
        super(engines, SerializerType.TREATEDBUILD);

        this.runId = runId;
        this.containerId = "N/A";
        this.status = "DETECTED";
        this.serialize();
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    protected abstract List<Object> serializeAsList();

    protected abstract JsonElement serializeAsJson();

    public void serialize() {
        SerializedData data = new SerializedData(this.serializeAsList(), this.serializeAsJson());

        List<SerializedData> allData = new ArrayList<>();
        allData.add(data);

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(allData, this.getType());
        }
    }
}
