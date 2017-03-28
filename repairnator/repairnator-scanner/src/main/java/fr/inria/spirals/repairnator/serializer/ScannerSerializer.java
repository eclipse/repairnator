package fr.inria.spirals.repairnator.serializer;

import com.google.api.services.sheets.v4.Sheets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.scanner.ProjectScanner;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by urli on 02/02/2017.
 */
public class ScannerSerializer extends ProcessSerializer {

    private ProjectScanner scanner;

    public ScannerSerializer(List<SerializerEngine> engines, ProjectScanner scanner) {
        super(engines, SerializerType.SCANNER);
        this.scanner = scanner;
    }

    private List<Object> serializeAsList() {
        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.formatCompleteDate(this.scanner.getScannerRunningBeginDate()));
        dataCol.add(Utils.formatCompleteDate(this.scanner.getLookFromDate()));
        dataCol.add(this.scanner.getTotalRepoNumber());
        dataCol.add(this.scanner.getTotalRepoUsingTravis());
        dataCol.add(this.scanner.getTotalScannedBuilds());
        dataCol.add(this.scanner.getTotalBuildInJava());
        dataCol.add(this.scanner.getTotalJavaPassingBuilds());
        dataCol.add(this.scanner.getTotalBuildInJavaFailing());
        dataCol.add(this.scanner.getTotalBuildInJavaFailingWithFailingTests());
        dataCol.add(this.scanner.getTotalPRBuilds());
        dataCol.add(Utils.formatOnlyDay(this.scanner.getLookFromDate()));
        dataCol.add(this.scanner.getScannerDuration());
        dataCol.add(this.scanner.getRunId());

        return dataCol;
    }

    private JsonElement serializeAsJson() {
        JsonObject result = new JsonObject();

        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("dateBegin", Utils.formatCompleteDate(this.scanner.getScannerRunningBeginDate()));
        result.addProperty("dateLimit", Utils.formatCompleteDate(this.scanner.getLookFromDate()));
        result.addProperty("dayLimit", Utils.formatOnlyDay(this.scanner.getLookFromDate()));
        result.addProperty("totalRepoNumber", this.scanner.getTotalRepoNumber());
        result.addProperty("totalRepoUsingTravis", this.scanner.getTotalRepoUsingTravis());
        result.addProperty("totalScannedBuilds", this.scanner.getTotalScannedBuilds());
        result.addProperty("totalJavaBuilds", this.scanner.getTotalBuildInJava());
        result.addProperty("totalJavaPassingBuilds", this.scanner.getTotalJavaPassingBuilds());
        result.addProperty("totalJavaFailingBuilds", this.scanner.getTotalBuildInJavaFailing());
        result.addProperty("totalJavaFailingBuildsWithFailingTests", this.scanner.getTotalBuildInJavaFailingWithFailingTests());
        result.addProperty("totalPRBuilds", this.scanner.getTotalPRBuilds());
        result.addProperty("duration", this.scanner.getScannerDuration());
        result.addProperty("runId", this.scanner.getRunId());

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

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        GoogleSpreadSheetFactory.initWithFileSecret("client_secret.json");
        GoogleSpreadSheetFactory.setSpreadsheetId(args[1]);

        Sheets sheets = GoogleSpreadSheetFactory.getSheets();

        List<List<Object>> results = sheets.spreadsheets().values().get(GoogleSpreadSheetFactory.getSpreadsheetID(), "Scanner Data!A:M").execute().getValues();

        MongoConnection mongoConnection = new MongoConnection(args[0],"repairnator");

        if (!mongoConnection.isConnected()) {
            throw new RuntimeException("Error when connection to mongodb");
        }

        MongoDBSerializerEngine serializerEngine = new MongoDBSerializerEngine(mongoConnection);

        List<SerializedData> data = new ArrayList<>();

        for (int i = 1; i < results.size(); i++) {
            List<Object> value = results.get(i);

            JsonObject result = new JsonObject();

            result.addProperty("hostname", Utils.getValue(value, 0));
            result.addProperty("dateBegin", Utils.getValue(value, 1));
            result.addProperty("dateLimit", Utils.getValue(value, 2));
            result.addProperty("totalRepoNumber", Utils.getValue(value, 3));
            result.addProperty("totalRepoUsingTravis", Utils.getValue(value, 4));
            result.addProperty("totalScannedBuilds", Utils.getValue(value, 5));
            result.addProperty("totalJavaBuilds", Utils.getValue(value, 6));
            result.addProperty("totalJavaPassingBuilds", Utils.getValue(value, 7));
            result.addProperty("totalJavaFailingBuilds", Utils.getValue(value, 8));
            result.addProperty("totalJavaFailingBuildsWithFailingTests", Utils.getValue(value, 9));
            result.addProperty("totalPRBuilds", Utils.getValue(value, 10));
            result.addProperty("dayLimit", Utils.getValue(value, 11));
            result.addProperty("duration", Utils.getValue(value, 12));
            result.addProperty("runId", Utils.getValue(value, 13));

            data.add(new SerializedData(Collections.EMPTY_LIST, result));
        }

        serializerEngine.serialize(data, SerializerType.SCANNER);
    }

}
