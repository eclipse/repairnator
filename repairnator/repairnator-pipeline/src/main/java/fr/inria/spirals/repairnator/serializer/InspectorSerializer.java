package fr.inria.spirals.repairnator.serializer;

import com.google.api.services.sheets.v4.Sheets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
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
 * Created by urli on 02/02/2017.
 */
public class InspectorSerializer extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(InspectorSerializer.class);

    public InspectorSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.INSPECTOR);
    }

    private List<Object> serializeAsList(ProjectInspector inspector) {
        JobStatus jobStatus = inspector.getJobStatus();
        BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();
        Build build = inspector.getBuild();

        String state = this.getPrettyPrintState(jobStatus);

        String realState = (jobStatus.getState() != null) ? jobStatus.getState().name() : "null";
        String typeOfFailures = StringUtils.join(jobStatus.getFailureNames(), ",");

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId() + "");
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(state);
        dataCol.add(build.getPullRequestNumber() + "");
        dataCol.add(Utils.formatCompleteDate(build.getFinishedAt()));
        dataCol.add(Utils.formatOnlyDay(build.getFinishedAt()));
        dataCol.add(realState);
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.formatCompleteDate(new Date()));
        dataCol.add(Utils.getTravisUrl(build.getId(), build.getRepository().getSlug()));
        dataCol.add(typeOfFailures);
        dataCol.add(buildToBeInspected.getRunId());

        return dataCol;
    }

    private JsonElement serializeAsJson(ProjectInspector inspector) {
        JobStatus jobStatus = inspector.getJobStatus();
        BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();
        Build build = inspector.getBuild();

        String state = this.getPrettyPrintState(jobStatus);

        String realState = (jobStatus.getState() != null) ? jobStatus.getState().name() : "null";
        String typeOfFailures = StringUtils.join(jobStatus.getFailureNames(), ",");

        JsonObject result = new JsonObject();
        result.addProperty("buildId", build.getId());
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("status", state);
        result.addProperty("prNumber", build.getPullRequestNumber());
        result.addProperty("buildFinishedDate", Utils.formatCompleteDate(build.getFinishedAt()));
        result.addProperty("buildFinishedDay", Utils.formatOnlyDay(build.getFinishedAt()));
        result.addProperty("realStatus", realState);
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("buildReproductionDate", Utils.formatCompleteDate(new Date()));
        result.addProperty("travisURL", Utils.getTravisUrl(build.getId(), build.getRepository().getSlug()));
        result.addProperty("typeOfFailures", typeOfFailures);
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
        Sheets sheets = GoogleSpreadSheetFactory.getSheets();

        List<List<Object>> results = sheets.spreadsheets().values().get(GoogleSpreadSheetFactory.getSpreadsheetID(), "All data!A:L").execute().getValues();

        MongoConnection mongoConnection = new MongoConnection(args[0],"repairnator");

        if (!mongoConnection.isConnected()) {
            throw new RuntimeException("Error when connection to mongodb");
        }

        MongoDBSerializerEngine serializerEngine = new MongoDBSerializerEngine(mongoConnection);

        List<SerializedData> data = new ArrayList<>();

        for (int i = 1; i < results.size(); i++) {
            List<Object> value = results.get(i);

            JsonObject result = new JsonObject();

            result.addProperty("buildId", Utils.getValue(value, 0));
            result.addProperty("repositoryName", Utils.getValue(value, 1));
            result.addProperty("status", Utils.getValue(value, 2));
            result.addProperty("prNumber", Utils.getValue(value, 3));
            result.addProperty("buildFinishedDate", Utils.getValue(value, 4));
            result.addProperty("buildFinishedDay", Utils.getValue(value, 5));
            result.addProperty("realStatus", Utils.getValue(value, 6));
            result.addProperty("hostname", Utils.getValue(value, 7));
            result.addProperty("buildReproductionDate", Utils.getValue(value, 8));
            result.addProperty("travisURL", Utils.getValue(value, 9));
            result.addProperty("typeOfFailures", Utils.getValue(value, 10));
            result.addProperty("runId", Utils.getValue(value, 11));

            data.add(new SerializedData(Collections.EMPTY_LIST, result));
        }

        serializerEngine.serialize(data, SerializerType.INSPECTOR);
    }
}
