package fr.inria.spirals.repairnator.realtime.serializer;

import com.google.gson.JsonObject;
import fr.inria.jtravis.entities.Repository;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.realtime.RTScanner;
import fr.inria.spirals.repairnator.serializer.Serializer;
import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BlacklistedSerializer extends Serializer {

    public enum Reason {
        OTHER_LANGUAGE,
        USE_GRADLE,
        UNKNOWN_BUILD_TOOL,
        NO_SUCCESSFUL_BUILD
    }

    RTScanner rtScanner;
    public BlacklistedSerializer(List<SerializerEngine> engines, RTScanner rtScanner) {
        super(engines, SerializerType.BLACKLISTED);
        this.rtScanner = rtScanner;
    }

    private List<Object> serializeAsList(Repository repo, Reason reason, String comment)  {
        List<Object> result = new ArrayList<>();
        result.add(Utils.getHostname());
        result.add(this.rtScanner.getRunId());
        result.add(Utils.formatCompleteDate(new Date()));
        result.add(repo.getId());
        result.add(repo.getSlug());
        result.add(reason.name());
        result.add(comment);
        return result;
    }

    private JsonObject serializeAsJson(Repository repo, Reason reason, String comment) {
        JsonObject result = new JsonObject();

        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("runId", this.rtScanner.getRunId());
        this.addDate(result, "dateBlacklist", new Date());
        result.addProperty("dateBlacklistStr", Utils.formatCompleteDate(new Date()));
        result.addProperty("repoId", repo.getId());
        result.addProperty("repoName", repo.getSlug());
        result.addProperty("reason", reason.name());
        result.addProperty("comment", comment);

        return result;
    }

    public void serialize(Repository repo, Reason reason, String comment) {
        SerializedData data = new SerializedData(this.serializeAsList(repo, reason, comment), this.serializeAsJson(repo, reason, comment));

        List<SerializedData> allData = new ArrayList<>();
        allData.add(data);

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(allData, this.getType());
        }
    }
}
