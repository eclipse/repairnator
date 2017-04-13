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
 * Created by fernanda on 06/03/2017.
 */
public class ScannerSerializer4Bears extends ProcessSerializer {
    private ProjectScanner scanner;

    public ScannerSerializer4Bears(List<SerializerEngine> engines, ProjectScanner scanner) {
        super(engines, SerializerType.SCANNER4BEARS);
        this.scanner = scanner;
    }

    private List<Object> serializeAsList() {
        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.formatCompleteDate(this.scanner.getScannerRunningBeginDate()));
        dataCol.add(Utils.formatCompleteDate(this.scanner.getScannerRunningEndDate()));
        dataCol.add(this.scanner.getScannerDuration());
        dataCol.add(Utils.formatCompleteDate(this.scanner.getLookFromDate()));
        dataCol.add(Utils.formatCompleteDate(this.scanner.getLookToDate()));
        dataCol.add(this.scanner.getTotalRepoNumber());
        dataCol.add(this.scanner.getTotalRepoUsingTravis());
        dataCol.add(this.scanner.getTotalScannedBuilds());
        dataCol.add(this.scanner.getTotalBuildInJava());
        dataCol.add(this.scanner.getTotalJavaPassingBuilds());
        dataCol.add(this.scanner.getTotalBuildInJavaFailing());
        dataCol.add(this.scanner.getTotalBuildInJavaFailingWithFailingTests());
        dataCol.add(this.scanner.getTotalNumberOfFailingAndPassingBuildPairs());
        dataCol.add(this.scanner.getTotalNumberOfPassingAndPassingBuildPairs());
        dataCol.add(this.scanner.getTotalNumberOfFailingAndPassingBuildPairs() + this.scanner.getTotalNumberOfPassingAndPassingBuildPairs());
        dataCol.add(this.scanner.getTotalPRBuilds());
        dataCol.add(Utils.formatOnlyDay(this.scanner.getLookFromDate()));
        dataCol.add(this.scanner.getRunId());

        return dataCol;
    }

    private JsonElement serializeAsJson() {
        JsonObject result = new JsonObject();

        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("dateBeginStr", Utils.formatCompleteDate(this.scanner.getScannerRunningBeginDate()));
        this.addDate(result, "dateBegin", this.scanner.getScannerRunningBeginDate());

        result.addProperty("dateEndStr", Utils.formatCompleteDate(this.scanner.getScannerRunningEndDate()));
        this.addDate(result, "dateEnd", this.scanner.getScannerRunningEndDate());

        result.addProperty("dateLimitStr", Utils.formatCompleteDate(this.scanner.getLookFromDate()));
        this.addDate(result, "dateLimit", this.scanner.getLookFromDate());

        result.addProperty("dayLimit", Utils.formatOnlyDay(this.scanner.getLookFromDate()));
        result.addProperty("totalRepoNumber", this.scanner.getTotalRepoNumber());
        result.addProperty("totalRepoUsingTravis", this.scanner.getTotalRepoUsingTravis());
        result.addProperty("totalScannedBuilds", this.scanner.getTotalScannedBuilds());
        result.addProperty("totalJavaBuilds", this.scanner.getTotalBuildInJava());
        result.addProperty("totalJavaPassingBuilds", this.scanner.getTotalJavaPassingBuilds());
        result.addProperty("totalJavaFailingBuilds", this.scanner.getTotalBuildInJavaFailing());
        result.addProperty("totalJavaFailingBuildsWithFailingTests", this.scanner.getTotalBuildInJavaFailingWithFailingTests());
        result.addProperty("totalFailingAndPassingBuildPairs", this.scanner.getTotalNumberOfFailingAndPassingBuildPairs());
        result.addProperty("totalPassingAndPassingBuildPairs", this.scanner.getTotalNumberOfPassingAndPassingBuildPairs());
        result.addProperty("totalPairOfBuilds", this.scanner.getTotalNumberOfFailingAndPassingBuildPairs() + this.scanner.getTotalNumberOfPassingAndPassingBuildPairs());
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

        MongoConnection mongoConnection = new MongoConnection(args[0],"bears");

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
            result.addProperty("dateEnd", Utils.getValue(value, 2));
            result.addProperty("duration", Utils.getValue(value, 3));
            //result.addProperty("dayLimit", Utils.getValue(value, 4));
            result.addProperty("dateLimit", Utils.getValue(value, 5));
            result.addProperty("totalRepoNumber", Utils.getValue(value, 6));
            result.addProperty("totalRepoUsingTravis", Utils.getValue(value, 7));
            result.addProperty("totalScannedBuilds", Utils.getValue(value, 8));
            result.addProperty("totalJavaBuilds", Utils.getValue(value, 9));
            result.addProperty("totalJavaPassingBuilds", Utils.getValue(value, 10));
            result.addProperty("totalJavaFailingBuilds", Utils.getValue(value, 11));
            result.addProperty("totalJavaFailingBuildsWithFailingTests", Utils.getValue(value, 12));
            result.addProperty("totalFailingAndPassingBuildPairs", Utils.getValue(value, 13));
            result.addProperty("totalPassingAndPassingBuildPairs", Utils.getValue(value, 14));
            result.addProperty("totalPairOfBuilds", Utils.getValue(value, 15));
            result.addProperty("totalPRBuilds", Utils.getValue(value, 16));
            result.addProperty("dayLimit", Utils.getValue(value, 17));
            result.addProperty("runId", Utils.getValue(value, 18));

            data.add(new SerializedData(Collections.EMPTY_LIST, result));
        }

        serializerEngine.serialize(data, SerializerType.SCANNER4BEARS);
    }

}
