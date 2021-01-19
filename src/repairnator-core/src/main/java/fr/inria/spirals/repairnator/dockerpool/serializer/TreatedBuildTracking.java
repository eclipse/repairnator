package fr.inria.spirals.repairnator.dockerpool.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.InputBuild;
import fr.inria.spirals.repairnator.serializer.SerializerImpl;
import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.utils.DateUtils;
import fr.inria.spirals.repairnator.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TreatedBuildTracking extends SerializerImpl {
    protected String buildId;
    protected String runId;
    protected String containerId;
    protected String status;

    public TreatedBuildTracking(List<SerializerEngine> engines, String runId, InputBuild build) {
        super(engines, SerializerType.TREATEDBUILD);
        this.buildId = build.toString();
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

    protected List<Object> serializeAsList() {
        Date date = new Date();

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(runId);
        dataCol.add(buildId);
        dataCol.add(containerId);
        dataCol.add(DateUtils.formatCompleteDate(date));
        dataCol.add(DateUtils.formatOnlyDay(date));
        dataCol.add(Utils.getHostname());
        dataCol.add(status);
        return dataCol;
    }

    protected JsonElement serializeAsJson() {
        Date date = new Date();

        JsonObject result = new JsonObject();
        result.addProperty("runId", runId);
        result.addProperty("buildId", buildId);
        result.addProperty("containerId", containerId);
        result.addProperty("dateReproducedBuildStr", DateUtils.formatCompleteDate(date));
        this.addDate(result, "dateReproducedBuild", date);

        result.addProperty("dayReproducedBuild", DateUtils.formatOnlyDay(date));
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("status", status);

        return result;
    }

    public void serialize() {
        SerializedData data = new SerializedData(this.serializeAsList(), this.serializeAsJson());

        List<SerializedData> allData = new ArrayList<>();
        allData.add(data);

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(allData, this.getType());
        }
    }
}
