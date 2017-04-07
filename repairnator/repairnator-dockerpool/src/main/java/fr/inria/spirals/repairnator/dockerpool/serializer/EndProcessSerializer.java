package fr.inria.spirals.repairnator.dockerpool.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.serializer.ProcessSerializer;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 16/02/2017.
 */
public class EndProcessSerializer extends ProcessSerializer {


    private Date beginDate;
    private String status;
    private int nbBuilds;
    private String runId;

    public EndProcessSerializer(List<SerializerEngine> engines, String runId) {
        super(engines, SerializerType.ENDPROCESS);
        this.beginDate = new Date();
        this.status = "unknown";
        this.runId = runId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setNbBuilds(int nbBuilds) {
        this.nbBuilds = nbBuilds;
    }

    private List<Object> serializeAsList() {
        Date now = new Date();
        String humanDuration = Utils.getDuration(this.beginDate, now);

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(this.runId);
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.formatOnlyDay(this.beginDate));
        dataCol.add(Utils.formatCompleteDate(this.beginDate));
        dataCol.add(Utils.formatCompleteDate(now));
        dataCol.add(humanDuration);
        dataCol.add(this.status);
        dataCol.add(this.nbBuilds);

        return dataCol;
    }

    private JsonElement serializeAsJson() {
        JsonObject result = new JsonObject();

        Date now = new Date();
        String humanDuration = Utils.getDuration(this.beginDate, now);

        result.addProperty("runId", this.runId);
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("beginDay", Utils.formatOnlyDay(this.beginDate));
        result.addProperty("beginDateStr", Utils.formatCompleteDate(this.beginDate));
        this.addDate(result, "beginDate", this.beginDate);

        result.addProperty("endDateStr", Utils.formatCompleteDate(now));
        this.addDate(result, "endDate", now);

        result.addProperty("duration", humanDuration);
        result.addProperty("endStatus", this.status);
        result.addProperty("nbBuilds", this.nbBuilds);

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
