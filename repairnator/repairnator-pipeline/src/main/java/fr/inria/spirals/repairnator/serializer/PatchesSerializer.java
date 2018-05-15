package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PatchesSerializer extends AbstractDataSerializer {

    public PatchesSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.PATCHES);
    }

    private List<Object> serializeAsList(BuildToBeInspected buildToBeInspected, RepairPatch patch) {
        Build build = buildToBeInspected.getBuggyBuild();

        List<Object> result = new ArrayList<>();
        result.add(Utils.formatCompleteDate(new Date()));
        result.add(buildToBeInspected.getRunId());
        result.add(build.getId());
        result.add(patch.getToolname());
        result.add(patch.getDiff());
        result.add(patch.getFilePath());
        result.add(Utils.getHostname());

        return result;
    }

    private JsonElement serializeAsJson(BuildToBeInspected buildToBeInspected, RepairPatch patch) {
        Build build = buildToBeInspected.getBuggyBuild();
        JsonObject data = new JsonObject();
        data.addProperty("date", Utils.formatCompleteDate(new Date()));
        this.addDate(data, "dateStr", new Date());
        data.addProperty("runId", buildToBeInspected.getRunId());
        data.addProperty("buildId", build.getId());
        data.addProperty("toolname", patch.getToolname());
        data.addProperty("diff", patch.getDiff());
        data.addProperty("filepath", patch.getFilePath());
        data.addProperty("hostname", Utils.getHostname());

        return data;
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        List<SerializedData> allData = new ArrayList<>();

        for (RepairPatch repairPatch : inspector.getJobStatus().getAllPatches()) {
            allData.add(new SerializedData(this.serializeAsList(inspector.getBuildToBeInspected(), repairPatch), this.serializeAsJson(inspector.getBuildToBeInspected(), repairPatch)));
        }

        if (!allData.isEmpty()) {
            for (SerializerEngine engine : this.getEngines()) {
                engine.serialize(allData, this.getType());
            }
        }
    }
}
