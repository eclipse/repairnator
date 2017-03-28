package fr.inria.spirals.repairnator.serializer;

import com.google.api.services.sheets.v4.Sheets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.lille.repair.common.config.NopolContext;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.nopol.NopolStatus;
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
import java.util.List;

/**
 * Created by urli on 16/02/2017.
 */
public class NopolSerializer extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(NopolSerializer.class);

    public NopolSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.NOPOL);
    }


    private List<Object> serializeNopolInfoAsList(BuildToBeInspected buildToBeInspected, NopolInformation nopolInformation, Patch patch, int patchNumber) {

        Build build = buildToBeInspected.getBuild();
        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.formatCompleteDate(nopolInformation.getDateEnd()));
        dataCol.add(Utils.formatOnlyDay(nopolInformation.getDateEnd()));
        dataCol.add(build.getId());
        dataCol.add(build.getRepository().getSlug());

        dataCol.add(nopolInformation.getLocation().getClassName());
        dataCol.add(StringUtils.join(nopolInformation.getLocation().getFailures(), ","));
        dataCol.add(nopolInformation.getAllocatedTime());
        dataCol.add(nopolInformation.getPassingTime());
        dataCol.add(nopolInformation.getStatus().name());

        if (nopolInformation.getStatus() == NopolStatus.EXCEPTION) {
            dataCol.add(nopolInformation.getExceptionDetail());
        } else {
            dataCol.add("N/A");
        }

        if (patch == null) {
            dataCol.add("N/A");
            dataCol.add("N/A");
            dataCol.add("N/A");
            dataCol.add("N/A");
        } else {
            dataCol.add(patchNumber + "/" + nopolInformation.getPatches().size());
            dataCol.add(patch.getType().name());
            dataCol.add(patch.asString());
            dataCol.add(patch.getRootClassName() + ":" + patch.getLineNumber());
        }

        NopolContext nopolContext = nopolInformation.getNopolContext();
        dataCol.add("localizer: " + nopolContext.getLocalizer().name() + ";solver: " + nopolContext.getSolver().name()
                + ";synthetizer: " + nopolContext.getSynthesis().name() + ";type: " + nopolContext.getType().name());
        dataCol.add(nopolInformation.getNbAngelicValues());
        dataCol.add(nopolInformation.getNbStatements());
        dataCol.add(nopolInformation.getIgnoreStatus().name());
        dataCol.add(buildToBeInspected.getRunId());
        return dataCol;
    }

    private JsonElement serializeNopolInfoAsJson(BuildToBeInspected buildToBeInspected, NopolInformation nopolInformation, Patch patch, int patchNumber) {
        Build build = buildToBeInspected.getBuild();

        JsonObject result = new JsonObject();

        result.addProperty("buildId", build.getId());
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("nopolDateEnd", Utils.formatCompleteDate(nopolInformation.getDateEnd()));
        result.addProperty("nopolDayEnd", Utils.formatOnlyDay(nopolInformation.getDateEnd()));
        result.addProperty("testClassLocation", nopolInformation.getLocation().getClassName());
        result.addProperty("failures", StringUtils.join(nopolInformation.getLocation().getFailures(), ","));
        result.addProperty("allocatedTime", nopolInformation.getAllocatedTime());
        result.addProperty("passingTime", nopolInformation.getPassingTime());
        result.addProperty("status", nopolInformation.getStatus().name());

        if (nopolInformation.getStatus() == NopolStatus.EXCEPTION) {
            result.addProperty("exceptionDetail", nopolInformation.getExceptionDetail());
        }

        if (patch != null) {
            result.addProperty("totalPatches", nopolInformation.getPatches().size());
            result.addProperty("patchNumber", patchNumber);
            result.addProperty("patchType", patch.getType().name());
            result.addProperty("patch", patch.asString());
            result.addProperty("patchLocation", patch.getRootClassName() + ":" + patch.getLineNumber());
        }

        NopolContext nopolContext = nopolInformation.getNopolContext();
        result.addProperty("nopolContext", "localizer: " + nopolContext.getLocalizer().name() + ";solver: " + nopolContext.getSolver().name()
                + ";synthetizer: " + nopolContext.getSynthesis().name() + ";type: " + nopolContext.getType().name());
        result.addProperty("nbAngelicValues", nopolInformation.getNbAngelicValues());
        result.addProperty("nbStatements", nopolInformation.getNbStatements());
        result.addProperty("ignoreStatus", nopolInformation.getIgnoreStatus().name());
        result.addProperty("runId", buildToBeInspected.getRunId());

        return result;
    }

    @Override
    public void serializeData(ProjectInspector inspector){
        if (inspector.getJobStatus().getNopolInformations() != null) {
            BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();

            List<SerializedData> allDatas = new ArrayList<>();

            for (NopolInformation nopolInformation : inspector.getJobStatus().getNopolInformations()) {
                if (nopolInformation.getPatches().isEmpty()) {
                    SerializedData data = new SerializedData(this.serializeNopolInfoAsList(buildToBeInspected, nopolInformation, null, 0),
                            this.serializeNopolInfoAsJson(buildToBeInspected, nopolInformation, null, 0));
                    allDatas.add(data);
                } else {
                    int patchNumber = 1;

                    for (Patch patch : nopolInformation.getPatches()) {
                        SerializedData data = new SerializedData(this.serializeNopolInfoAsList(buildToBeInspected, nopolInformation, patch, patchNumber),
                                this.serializeNopolInfoAsJson(buildToBeInspected, nopolInformation, patch, patchNumber));
                        allDatas.add(data);
                        patchNumber++;
                    }
                }

                for (SerializerEngine engine : this.getEngines()) {
                    engine.serialize(allDatas, this.getType());
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        GoogleSpreadSheetFactory.initWithFileSecret("client_secret.json");
        GoogleSpreadSheetFactory.setSpreadsheetId(args[1]);

        Sheets sheets = GoogleSpreadSheetFactory.getSheets();

        List<List<Object>> results = sheets.spreadsheets().values().get(GoogleSpreadSheetFactory.getSpreadsheetID(), "Nopol Stats!A:T").execute().getValues();

        MongoConnection mongoConnection = new MongoConnection(args[0],"repairnator");

        if (!mongoConnection.isConnected()) {
            throw new RuntimeException("Error when connection to mongodb");
        }

        MongoDBSerializerEngine serializerEngine = new MongoDBSerializerEngine(mongoConnection);

        List<SerializedData> data = new ArrayList<>();

        for (int i = 1; i < results.size(); i++) {
            List<Object> value = results.get(i);

            JsonObject result = new JsonObject();

            result.addProperty("buildId", Utils.getValue(value, 3));
            result.addProperty("repositoryName", Utils.getValue(value, 4));
            result.addProperty("hostname", Utils.getValue(value, 0));
            result.addProperty("nopolDateEnd", Utils.getValue(value, 1));
            result.addProperty("nopolDayEnd", Utils.getValue(value, 2));
            result.addProperty("testClassLocation", Utils.getValue(value, 5));
            result.addProperty("failures", Utils.getValue(value, 6));
            result.addProperty("allocatedTime", Utils.getValue(value, 7));
            result.addProperty("passingTime", Utils.getValue(value, 8));
            result.addProperty("status", Utils.getValue(value, 9));
            result.addProperty("exceptionDetail", Utils.getValue(value, 10));
            result.addProperty("patchNumber", Utils.getValue(value, 11));
            result.addProperty("patchType", Utils.getValue(value, 12));
            result.addProperty("patch", Utils.getValue(value, 13));
            result.addProperty("patchLocation", Utils.getValue(value, 14));
            result.addProperty("nopolContext", Utils.getValue(value, 15));
            result.addProperty("nbAngelicValues", Utils.getValue(value, 16));
            result.addProperty("nbStatements", Utils.getValue(value, 17));
            result.addProperty("ignoreStatus", Utils.getValue(value, 18));
            result.addProperty("runId", Utils.getValue(value, 19));

            data.add(new SerializedData(Collections.EMPTY_LIST, result));
        }

        serializerEngine.serialize(data, SerializerType.NOPOL);
    }
}
