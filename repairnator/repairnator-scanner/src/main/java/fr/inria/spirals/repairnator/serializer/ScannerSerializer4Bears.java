package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.scanner.ProjectScanner;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import java.util.ArrayList;
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
        dataCol.add(this.scanner.getTotalPRBuilds());
        dataCol.add(this.scanner.getTotalNumberOfFailingAndPassingBuildPairs());
        dataCol.add(this.scanner.getTotalNumberOfPassingAndPassingBuildPairs());
        dataCol.add(this.scanner.getTotalNumberOfFailingAndPassingBuildPairs() + this.scanner.getTotalNumberOfPassingAndPassingBuildPairs());
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

        result.addProperty("duration", this.scanner.getScannerDuration());

        result.addProperty("dateLookedFromStr", Utils.formatCompleteDate(this.scanner.getLookFromDate()));
        this.addDate(result, "dateLookedFrom", this.scanner.getLookFromDate());

        result.addProperty("dateLookedToStr", Utils.formatCompleteDate(this.scanner.getLookToDate()));
        this.addDate(result, "dateLookedTo", this.scanner.getLookToDate());

        result.addProperty("totalRepoNumber", this.scanner.getTotalRepoNumber());
        result.addProperty("totalRepoUsingTravis", this.scanner.getTotalRepoUsingTravis());
        result.addProperty("totalScannedBuilds", this.scanner.getTotalScannedBuilds());
        result.addProperty("totalJavaBuilds", this.scanner.getTotalBuildInJava());
        result.addProperty("totalJavaPassingBuilds", this.scanner.getTotalJavaPassingBuilds());
        result.addProperty("totalJavaFailingBuilds", this.scanner.getTotalBuildInJavaFailing());
        result.addProperty("totalJavaFailingBuildsWithFailingTests", this.scanner.getTotalBuildInJavaFailingWithFailingTests());
        result.addProperty("totalPRBuilds", this.scanner.getTotalPRBuilds());
        result.addProperty("totalFailingAndPassingBuildPairs", this.scanner.getTotalNumberOfFailingAndPassingBuildPairs());
        result.addProperty("totalPassingAndPassingBuildPairs", this.scanner.getTotalNumberOfPassingAndPassingBuildPairs());
        result.addProperty("totalPairOfBuilds", this.scanner.getTotalNumberOfFailingAndPassingBuildPairs() + this.scanner.getTotalNumberOfPassingAndPassingBuildPairs());
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
}
