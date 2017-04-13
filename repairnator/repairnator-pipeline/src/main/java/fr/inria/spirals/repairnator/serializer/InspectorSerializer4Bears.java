package fr.inria.spirals.repairnator.serializer;

import com.google.api.services.sheets.v4.Sheets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by fermadeiral.
 */
public class InspectorSerializer4Bears extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(InspectorSerializer4Bears.class);

    public InspectorSerializer4Bears(List<SerializerEngine> engines) {
        super(engines, SerializerType.INSPECTOR4BEARS);
    }

    private List<Object> serializeAsList(ProjectInspector inspector) {
        JobStatus jobStatus = inspector.getJobStatus();
        BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();
        Build build = inspector.getBuild();

        Build previousBuild = inspector.getPreviousBuild();
        int previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

        String state = this.getPrettyPrintState(inspector);

        String realState = (jobStatus.getState() != null) ? jobStatus.getState().name() : "null";

        String typeOfFailures = StringUtils.join(jobStatus.getFailureNames(), ",")+"";
        String previousBuildSlug = (previousBuild != null) ? previousBuild.getRepository().getSlug() : "";

        String committerEmail = (build.getCommit().getCommitterEmail() != null) ? build.getCommit().getCommitterEmail() : "-";

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId() + "");
        dataCol.add(previousBuildId + "");
        dataCol.add(buildToBeInspected.getStatus().name());
        dataCol.add(state);
        dataCol.add(realState);
        dataCol.add(inspector.getCheckoutType().name());
        dataCol.add(typeOfFailures);
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(build.getPullRequestNumber() + "");
        dataCol.add(Utils.formatCompleteDate(build.getFinishedAt()));
        dataCol.add(Utils.formatOnlyDay(build.getFinishedAt()));
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.formatCompleteDate(new Date()));
        dataCol.add(Utils.getTravisUrl(build.getId(), build.getRepository().getSlug()));
        dataCol.add(Utils.getTravisUrl(previousBuildId, previousBuildSlug));
        dataCol.add(committerEmail);
        dataCol.add(buildToBeInspected.getRunId());

        return dataCol;
    }

    private JsonElement serializeAsJson(ProjectInspector inspector) {
        JobStatus jobStatus = inspector.getJobStatus();
        BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();
        Build build = inspector.getBuild();

        Build previousBuild = inspector.getPreviousBuild();
        int previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

        String state = this.getPrettyPrintState(inspector);

        String realState = (jobStatus.getState() != null) ? jobStatus.getState().name() : "null";

        String typeOfFailures = StringUtils.join(jobStatus.getFailureNames(), ",");
        String previousBuildSlug = (previousBuild != null) ? previousBuild.getRepository().getSlug() : "";

        String committerEmail = (build.getCommit().getCommitterEmail() != null) ? build.getCommit().getCommitterEmail() : "-";

        JsonObject result = new JsonObject();

        result.addProperty("buildId", build.getId());
        result.addProperty("previousBuildId", previousBuildId);
        result.addProperty("scannedBuildStatus", buildToBeInspected.getStatus().name());
        result.addProperty("status", state);
        result.addProperty("realStatus", realState);
        result.addProperty("checkoutType", inspector.getCheckoutType().name());
        result.addProperty("typeOfFailures", typeOfFailures);
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("prNumber", build.getPullRequestNumber());
        result.addProperty("buildFinishedDateStr", Utils.formatCompleteDate(build.getFinishedAt()));
        this.addDate(result, "buildFinishedDate", build.getFinishedAt());
        result.addProperty("buildFinishedDay", Utils.formatOnlyDay(build.getFinishedAt()));
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("buildReproductionDateStr", Utils.formatCompleteDate(new Date()));
        this.addDate(result, "buildReproductionDate", new Date());
        result.addProperty("buildTravisUrl", Utils.getTravisUrl(build.getId(), build.getRepository().getSlug()));
        result.addProperty("previousBuildTravisUrl", Utils.getTravisUrl(previousBuildId, previousBuildSlug));
        result.addProperty("committerEmail", committerEmail);
        result.addProperty("runId", buildToBeInspected.getRunId());

        return result;
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        SerializedData data = new SerializedData(this.serializeAsList(inspector), this.serializeAsJson(inspector));

        List<SerializedData> allData = new ArrayList<>();
        allData.add(data);

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(allData, this.getType());
        }
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        GoogleSpreadSheetFactory.initWithFileSecret("client_secret.json");
        GoogleSpreadSheetFactory.setSpreadsheetId(args[1]);

        Sheets sheets = GoogleSpreadSheetFactory.getSheets();

        List<List<Object>> results = sheets.spreadsheets().values().get(GoogleSpreadSheetFactory.getSpreadsheetID(), "All data!A:Q").execute().getValues();

        MongoConnection mongoConnection = new MongoConnection(args[0],"bears");

        if (!mongoConnection.isConnected()) {
            throw new RuntimeException("Error when connection to mongodb");
        }

        MongoDBSerializerEngine serializerEngine = new MongoDBSerializerEngine(mongoConnection);

        List<SerializedData> data = new ArrayList<>();

        for (int i = 1; i < results.size(); i++) {
            List<Object> value = results.get(i);

            JsonObject result = new JsonObject();

            result.addProperty("buildId", Utils.getValue(value, 0));
            result.addProperty("previousBuildId", Utils.getValue(value, 1));
            result.addProperty("scannedBuildStatus", Utils.getValue(value, 2));
            result.addProperty("status", Utils.getValue(value, 3));
            result.addProperty("realStatus", Utils.getValue(value, 4));
            result.addProperty("checkoutType", Utils.getValue(value, 5));
            result.addProperty("typeOfFailures", Utils.getValue(value, 6));
            result.addProperty("repositoryName", Utils.getValue(value, 7));
            result.addProperty("prNumber", Utils.getValue(value, 8));
            result.addProperty("buildFinishedDate", Utils.getValue(value, 9));
            result.addProperty("buildFinishedDay", Utils.getValue(value, 10));
            result.addProperty("hostname", Utils.getValue(value, 11));
            result.addProperty("buildReproductionDate", Utils.getValue(value, 12));
            result.addProperty("buildTravisUrl", Utils.getValue(value, 13));
            result.addProperty("previousBuildTravisUrl", Utils.getValue(value, 14));
            result.addProperty("committerEmail", Utils.getValue(value, 15));
            result.addProperty("runId", Utils.getValue(value, 16));

            data.add(new SerializedData(Collections.EMPTY_LIST, result));
        }

        serializerEngine.serialize(data, SerializerType.INSPECTOR4BEARS);
    }
}
