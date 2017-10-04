package fr.inria.spirals.repairnator.serializer;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 04/10/2017.
 */
public class AstorSerializer extends AbstractDataSerializer {

    public AstorSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.ASTOR);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        JsonElement result = inspector.getJobStatus().getAstorResults();

        if (result != null) {
            List<Object> dataAsList = new ArrayList<>();
            dataAsList.add(Utils.getHostname());
            dataAsList.add(inspector.getBuildToBeInspected().getRunId());
            dataAsList.add(Utils.formatCompleteDate(new Date()));
            dataAsList.add(Utils.formatOnlyDay(new Date()));
            dataAsList.add(inspector.getRepoSlug());
            dataAsList.add(inspector.getBuggyBuild().getId());
            dataAsList.add(result.toString());

            JsonObject dataAsJson = new JsonObject();
            dataAsJson.addProperty("hostname", Utils.getHostname());
            dataAsJson.addProperty("runId", inspector.getBuildToBeInspected().getRunId());
            dataAsJson.addProperty("buildId", inspector.getBuggyBuild().getId());
            dataAsJson.addProperty("repositoryName", inspector.getRepoSlug());
            this.addDate(dataAsJson, "computedDate", new Date());
            dataAsJson.addProperty("computedDateStr", Utils.formatCompleteDate(new Date()));
            dataAsJson.addProperty("computedDay", Utils.formatOnlyDay(new Date()));
            dataAsJson.add("result", result);

            List<SerializedData> serializedData = new ArrayList<>();
            serializedData.add(new SerializedData(dataAsList, dataAsJson));

            for (SerializerEngine engine : this.getEngines()) {
                engine.serialize(serializedData, this.getType());
            }
        }
    }
}
