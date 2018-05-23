package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by fernanda on 13/03/17.
 */
public class ScannerDetailedDataSerializer extends ProcessSerializer {

    private Map<ScannedBuildStatus, List<BuildToBeInspected>> buildsToBeInspected;

    public ScannerDetailedDataSerializer(List<SerializerEngine> engines, Map<ScannedBuildStatus, List<BuildToBeInspected>> buildsToBeInspected) {
        super(engines, SerializerType.DETAILEDDATA);
        this.buildsToBeInspected = buildsToBeInspected;
    }

    private List<Object> serializeAsList(BuildToBeInspected buildToBeInspected) {
        List<Object> dataCol = new ArrayList<Object>();

        Build build = buildToBeInspected.getPatchedBuild();
        Build previousBuild = buildToBeInspected.getBuggyBuild();
        long previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

        String committerEmail = "nobody@github.com";
        if (build.getCommitter().isPresent()) {
            committerEmail = build.getCommitter().get().getEmail();
        }

        Date date = new Date();
        dataCol.add(build.getId() + "");
        dataCol.add(previousBuildId + "");
        dataCol.add(buildToBeInspected.getStatus().name());
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(Utils.formatCompleteDate(date));
        dataCol.add(Utils.formatOnlyDay(date));
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.getTravisUrl(build.getId(), build.getRepository().getSlug()));
        dataCol.add(Utils.getTravisUrl(previousBuildId, build.getRepository().getSlug()));
        dataCol.add(committerEmail);
        dataCol.add(buildToBeInspected.getRunId());

        return dataCol;
    }

    private JsonElement serializeAsJson(BuildToBeInspected buildToBeInspected) {
        JsonObject result = new JsonObject();

        Build build = buildToBeInspected.getPatchedBuild();
        Build previousBuild = buildToBeInspected.getBuggyBuild();
        long previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

        String committerEmail = "nobody@github.com";
        if (build.getCommitter().isPresent()) {
            committerEmail = build.getCommitter().get().getEmail();
        }

        Date date = new Date();
        result.addProperty("buildId", build.getId());
        result.addProperty("previousBuildId", previousBuildId);
        result.addProperty("scannedStatus", buildToBeInspected.getStatus().name());
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("dateScannedStr", Utils.formatCompleteDate(date));
        this.addDate(result, "dateScanned", date);
        result.addProperty("dayScanned", Utils.formatOnlyDay(date));
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("travisBuildUrl", Utils.getTravisUrl(build.getId(), build.getRepository().getSlug()));
        result.addProperty("travisPreviousBuildUrl", Utils.getTravisUrl(previousBuildId, build.getRepository().getSlug()));
        result.addProperty("committerEmail", committerEmail);
        result.addProperty("runId", buildToBeInspected.getRunId());

        return result;
    }

    public void serialize() {
        if (!this.buildsToBeInspected.isEmpty()) {
            List<SerializedData> allData = new ArrayList<>();

            for (ScannedBuildStatus status : ScannedBuildStatus.values()) {
                for (BuildToBeInspected buildToBeInspected : this.buildsToBeInspected.get(status)) {
                    allData.add(new SerializedData(this.serializeAsList(buildToBeInspected), this.serializeAsJson(buildToBeInspected)));
                }
            }

            for (SerializerEngine engine : this.getEngines()) {
                engine.serialize(allData, this.getType());
            }
        }
    }

}
