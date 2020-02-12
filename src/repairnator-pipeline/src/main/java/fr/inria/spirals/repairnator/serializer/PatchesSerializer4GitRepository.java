package fr.inria.spirals.repairnator.serializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.utils.DateUtils;
import fr.inria.spirals.repairnator.utils.Utils;

public class PatchesSerializer4GitRepository extends AbstractDataSerializer {
	
	public PatchesSerializer4GitRepository(List<SerializerEngine> engines, ProjectInspector inspector) {
        super(engines, SerializerType.PATCHES, inspector);
    }

    private List<Object> serializeAsList(RepairPatch patch) {
        
    	List<Object> result = new ArrayList<>();
        result.add(DateUtils.formatCompleteDate(new Date()));
        result.add(inspector.getGitRepositoryId());
        result.add(patch.getToolname());
        result.add(patch.getDiff());
        result.add(patch.getFilePath());
        result.add(Utils.getHostname());

        return result;
    }

    private JsonElement serializeAsJson(RepairPatch patch) {
        
        JsonObject data = new JsonObject();
        data.addProperty("dateStr", DateUtils.formatCompleteDate(new Date()));
        this.addDate(data, "date", new Date());
        data.addProperty("repositoryId", inspector.getGitRepositoryId());
        data.addProperty("toolname", patch.getToolname());
        data.addProperty("diff", patch.getDiff());
        data.addProperty("filepath", patch.getFilePath());
        data.addProperty("hostname", Utils.getHostname());

        return data;
    }

    @Override
    public void serialize() {
        List<SerializedData> allData = new ArrayList<>();

        for (RepairPatch repairPatch : inspector.getJobStatus().getAllPatches()) {
            allData.add(new SerializedData(this.serializeAsList(repairPatch), this.serializeAsJson(repairPatch)));
        }

        if (!allData.isEmpty()) {
            for (SerializerEngine engine : this.getEngines()) {
                engine.serialize(allData, this.getType());
            }
        }
    }
}
