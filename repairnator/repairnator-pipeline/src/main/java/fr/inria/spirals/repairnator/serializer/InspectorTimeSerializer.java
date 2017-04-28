package fr.inria.spirals.repairnator.serializer;

import com.google.api.services.sheets.v4.Sheets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.ResolveDependency;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.NopolRepair;
import fr.inria.spirals.repairnator.process.step.push.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by urli on 02/02/2017.
 */
public class InspectorTimeSerializer extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(InspectorTimeSerializer.class);

    public InspectorTimeSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.TIMES);
    }

    private List<Object> serializeAsList(ProjectInspector inspector) {
        Map<String, Integer> durations = inspector.getJobStatus().getMetrics().getStepsDurationsInSeconds();

        int clonage = durations.getOrDefault(CloneRepository.class.getSimpleName(), 0);
        int checkoutBuild = durations.getOrDefault(CheckoutBuggyBuild.class.getSimpleName(), 0);
        int buildtime = durations.getOrDefault(BuildProject.class.getSimpleName(), 0);
        int test = durations.getOrDefault(TestProject.class.getSimpleName(), 0);
        int gatherTestInfo = durations.getOrDefault(GatherTestInformation.class.getSimpleName(), 0);
        int push = durations.getOrDefault(PushIncriminatedBuild.class.getSimpleName(), 0);
        int computeClasspath = durations.getOrDefault(ComputeClasspath.class.getSimpleName(), 0);
        int computeSourceDir = durations.getOrDefault(ComputeSourceDir.class.getSimpleName(), 0);
        int repair = durations.getOrDefault(NopolRepair.class.getSimpleName(), 0);
        int dependencyResolution = durations.getOrDefault(ResolveDependency.class.getSimpleName(), 0);

        int totalDuration = clonage + checkoutBuild + buildtime + test + gatherTestInfo + push +
                computeClasspath + computeSourceDir + repair + dependencyResolution;

        Build build = inspector.getBuggyBuild();

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId() + "");
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(Utils.formatCompleteDate(new Date()));
        dataCol.add(Utils.getHostname());
        dataCol.add(totalDuration);
        dataCol.add(clonage);
        dataCol.add(checkoutBuild);
        dataCol.add(buildtime);
        dataCol.add(test);
        dataCol.add(gatherTestInfo);
        dataCol.add(push);
        dataCol.add(computeClasspath);
        dataCol.add(computeSourceDir);
        dataCol.add(repair);
        dataCol.add(inspector.getBuildToBeInspected().getRunId());
        dataCol.add(dependencyResolution);
        return dataCol;
    }

    private JsonElement serializeAsJson(ProjectInspector inspector) {
        Map<String, Integer> durations = inspector.getJobStatus().getMetrics().getStepsDurationsInSeconds();

        int clonage = durations.getOrDefault(CloneRepository.class.getSimpleName(), 0);
        int checkoutBuild = durations.getOrDefault(CheckoutBuggyBuild.class.getSimpleName(), 0);
        int buildtime = durations.getOrDefault(BuildProject.class.getSimpleName(), 0);
        int test = durations.getOrDefault(TestProject.class.getSimpleName(), 0);
        int gatherTestInfo = durations.getOrDefault(GatherTestInformation.class.getSimpleName(), 0);
        int push = durations.getOrDefault(PushIncriminatedBuild.class.getSimpleName(), 0);
        int computeClasspath = durations.getOrDefault(ComputeClasspath.class.getSimpleName(), 0);
        int computeSourceDir = durations.getOrDefault(ComputeSourceDir.class.getSimpleName(), 0);
        int repair = durations.getOrDefault(NopolRepair.class.getSimpleName(), 0);
        int dependencyResolution = durations.getOrDefault(ResolveDependency.class.getSimpleName(), 0);

        int totalDuration = clonage + checkoutBuild + buildtime + test + gatherTestInfo + push +
                computeClasspath + computeSourceDir + repair + dependencyResolution;

        Build build = inspector.getBuggyBuild();

        JsonObject result = new JsonObject();

        result.addProperty("buildId", build.getId());
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("buildReproductionDateStr", Utils.formatCompleteDate(new Date()));
        this.addDate(result, "buildReproductionDate", new Date());

        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("totalDuration", totalDuration);
        result.addProperty("clonage", clonage);
        result.addProperty("checkoutBuild", checkoutBuild);
        result.addProperty("build", buildtime);
        result.addProperty("test", test);
        result.addProperty("gatherTestInfo", gatherTestInfo);
        result.addProperty("push", push);
        result.addProperty("computeClasspath", computeClasspath);
        result.addProperty("computeSourceDir", computeSourceDir);
        result.addProperty("repair", repair);
        result.addProperty("dependendencyResolution", dependencyResolution);
        result.addProperty("runId", inspector.getBuildToBeInspected().getRunId());

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

        List<List<Object>> results = sheets.spreadsheets().values().get(GoogleSpreadSheetFactory.getSpreadsheetID(), "Duration Data!A:P").execute().getValues();

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
            result.addProperty("buildReproductionDate", Utils.getValue(value, 2));
            result.addProperty("hostname", Utils.getValue(value, 3));
            result.addProperty("totalDuration", Utils.getValue(value, 4));
            result.addProperty("clonage", Utils.getValue(value, 5));
            result.addProperty("checkoutBuild", Utils.getValue(value, 6));
            result.addProperty("build", Utils.getValue(value, 7));
            result.addProperty("test", Utils.getValue(value, 8));
            result.addProperty("gatherTestInfo", Utils.getValue(value, 9));
            result.addProperty("squashRepository", Utils.getValue(value, 10));
            result.addProperty("push", Utils.getValue(value, 11));
            result.addProperty("computeClasspath", Utils.getValue(value, 12));
            result.addProperty("computeSourceDir", Utils.getValue(value, 13));
            result.addProperty("repair", Utils.getValue(value, 14));
            result.addProperty("runId", Utils.getValue(value, 15));

            data.add(new SerializedData(Collections.EMPTY_LIST, result));
        }

        serializerEngine.serialize(data, SerializerType.TIMES);
    }
}
