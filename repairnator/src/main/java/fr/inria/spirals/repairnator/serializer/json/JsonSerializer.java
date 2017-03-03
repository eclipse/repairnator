package fr.inria.spirals.repairnator.serializer.json;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.RepairMode;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.scanner.ProjectScanner;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.NopolRepair;
import fr.inria.spirals.repairnator.process.ProjectState;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by urli on 05/01/2017.
 */
public class JsonSerializer extends AbstractDataSerializer {

    private final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);
    private Gson serializer;
    private Date dateStart;
    private Date dateFinish;
    private ProjectScanner scanner;
    private String outputPath;
    private RepairMode mode;
    Map<String, JsonArray> inspectors;

    public JsonSerializer(String outputPath, RepairMode mode) {
        super();
        this.dateStart = new Date();
        this.outputPath = outputPath;

        this.serializer = new GsonBuilder().setPrettyPrinting().setExclusionStrategies(new CustomExclusionStrategy())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        this.mode = mode;
        this.inspectors = new HashMap<String, JsonArray>();

        for (ProjectState state : ProjectState.values()) {
            this.inspectors.put(state.name(), new JsonArray());
        }
    }

    public void setScanner(ProjectScanner scanner) {
        this.scanner = scanner;
    }

    private JsonElement serialize(Object object) {
        return this.serializer.toJsonTree(object);
    }

    private void writeFile(JsonObject root) throws IOException {
        String serialization = this.serializer.toJson(root);

        File outputFile = new File(this.outputPath);
        if (outputFile.isDirectory()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd_HHmmss");
            String formattedDate = dateFormat.format(new Date());
            String filename = "librepair_" + mode.name().toLowerCase() + "_" + formattedDate + ".json";
            outputFile = new File(outputFile.getPath() + File.separator + filename);
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        writer.write(serialization);
        writer.close();
    }

    private void outputNotClonableInspector(ProjectInspector inspector) {
        JsonObject result = new JsonObject();
        result.addProperty("slug", inspector.getRepoSlug());
        Build build = inspector.getBuild();
        result.addProperty("buildId", build.getId());
        result.add("buildDate", serialize(build.getFinishedAt()));
        result.add("stepsDuration", serialize(inspector.getStepsDurationsInSeconds()));
        if (build.isPullRequest()) {
            result.add("commit", serialize(build.getPRInformation()));
        } else {
            result.add("commit", serialize(build.getCommit()));
        }
        result.add("errors", serialize(inspector.getStepErrors()));
        this.inspectors.get(inspector.getState().name()).add(result);
    }

    private void outputNotBuildableInspector(ProjectInspector inspector) {
        JsonObject result = new JsonObject();
        result.addProperty("slug", inspector.getRepoSlug());
        Build build = inspector.getBuild();
        result.addProperty("buildId", build.getId());
        result.add("buildDate", serialize(build.getFinishedAt()));
        result.add("stepsDuration", serialize(inspector.getStepsDurationsInSeconds()));
        result.addProperty("localRepo", inspector.getRepoLocalPath());
        if (build.isPullRequest()) {
            result.add("commit", serialize(build.getPRInformation()));
        } else {
            result.add("commit", serialize(build.getCommit()));
        }
        result.add("errors", serialize(inspector.getStepErrors()));
        this.inspectors.get(inspector.getState().name()).add(result);
    }

    private void outputNotTestableInspector(ProjectInspector inspector) {
        JsonObject result = new JsonObject();
        result.addProperty("slug", inspector.getRepoSlug());
        Build build = inspector.getBuild();
        result.addProperty("buildId", build.getId());
        result.add("buildDate", serialize(build.getFinishedAt()));
        result.add("stepsDuration", serialize(inspector.getStepsDurationsInSeconds()));
        result.addProperty("localRepo", inspector.getRepoLocalPath());
        if (build.isPullRequest()) {
            result.add("commit", serialize(build.getPRInformation()));
        } else {
            result.add("commit", serialize(build.getCommit()));
        }
        result.add("errors", serialize(inspector.getStepErrors()));
        this.inspectors.get(inspector.getState().name()).add(result);
    }

    private void outputFailWhenGatheringInfoInspector(ProjectInspector inspector) {
        JsonObject result = new JsonObject();
        result.addProperty("slug", inspector.getRepoSlug());
        Build build = inspector.getBuild();
        result.addProperty("buildId", build.getId());
        result.add("buildDate", serialize(build.getFinishedAt()));
        result.add("stepsDuration", serialize(inspector.getStepsDurationsInSeconds()));
        result.addProperty("localRepo", inspector.getRepoLocalPath());
        if (build.isPullRequest()) {
            result.add("commit", serialize(build.getPRInformation()));
        } else {
            result.add("commit", serialize(build.getCommit()));
        }
        result.add("errors", serialize(inspector.getStepErrors()));
        this.inspectors.get(inspector.getState().name()).add(result);
    }

    private void outputHasTestFailureInspector(ProjectInspector inspector) {
        JsonObject result = new JsonObject();
        result.addProperty("slug", inspector.getRepoSlug());
        Build build = inspector.getBuild();
        result.addProperty("buildId", build.getId());
        result.add("buildDate", serialize(build.getFinishedAt()));
        result.add("stepsDuration", serialize(inspector.getStepsDurationsInSeconds()));
        result.addProperty("localRepo", inspector.getRepoLocalPath());

        NopolRepair nopolRepair = inspector.getNopolRepair();
        result.add("nopolInformations", serialize(nopolRepair.getNopolInformations()));

        GatherTestInformation testInformation = inspector.getTestInformations();
        result.addProperty("failingModulePath", testInformation.getFailingModulePath());
        result.addProperty("nbTests", testInformation.getNbTotalTests());
        result.addProperty("nbSkippingTests", testInformation.getNbSkippingTests());
        result.addProperty("nbFailingtests", testInformation.getNbFailingTests());
        result.addProperty("nbErroringTests", testInformation.getNbErroringTests());
        result.add("failureLocations", serialize(testInformation.getFailureLocations()));
        result.add("errors", serialize(inspector.getStepErrors()));

        this.inspectors.get(inspector.getState().name()).add(result);
    }

    private void outputNotFailingInspector(ProjectInspector inspector) {
        JsonObject result = new JsonObject();
        result.addProperty("slug", inspector.getRepoSlug());
        Build build = inspector.getBuild();
        result.addProperty("buildId", build.getId());
        result.add("buildDate", serialize(build.getFinishedAt()));
        result.add("stepsDuration", serialize(inspector.getStepsDurationsInSeconds()));
        result.addProperty("localRepo", inspector.getRepoLocalPath());

        GatherTestInformation testInformation = inspector.getTestInformations();
        result.addProperty("nbTests", testInformation.getNbTotalTests());
        result.addProperty("nbSkippingTests", testInformation.getNbSkippingTests());
        result.add("errors", serialize(inspector.getStepErrors()));
        this.inspectors.get(inspector.getState().name()).add(result);
    }

    private void outputHasBeenPatchedInspector(ProjectInspector inspector) {
        JsonObject result = new JsonObject();
        result.addProperty("slug", inspector.getRepoSlug());
        Build build = inspector.getBuild();
        result.addProperty("buildId", build.getId());
        result.add("buildDate", serialize(build.getFinishedAt()));
        result.add("stepsDuration", serialize(inspector.getStepsDurationsInSeconds()));
        result.addProperty("localRepo", inspector.getRepoLocalPath());

        NopolRepair nopolRepair = inspector.getNopolRepair();
        result.add("nopolInformations", serialize(nopolRepair.getNopolInformations()));

        GatherTestInformation testInformation = inspector.getTestInformations();
        result.addProperty("failingModulePath", testInformation.getFailingModulePath());
        result.addProperty("nbTests", testInformation.getNbTotalTests());
        result.addProperty("nbSkippingTests", testInformation.getNbSkippingTests());
        result.addProperty("nbFailingtests", testInformation.getNbFailingTests());
        result.add("failureLocations", serialize(testInformation.getFailureLocations()));
        result.add("errors", serialize(inspector.getStepErrors()));
        this.inspectors.get(inspector.getState().name()).add(result);
    }

    private void outputBuildWithoutPreviousBuildInspector(ProjectInspector inspector) {
        JsonObject result = new JsonObject();
        result.addProperty("slug", inspector.getRepoSlug());

        Build build = inspector.getBuild();
        result.addProperty("buildId", build.getId());
        result.add("buildDate", serialize(build.getFinishedAt()));

        result.addProperty("localRepo", inspector.getRepoLocalPath());

        this.inspectors.get(inspector.getState().name()).add(result);
    }

    private void outputFixerBuildInspector(ProjectInspector inspector) {
        if (inspector instanceof ProjectInspector4Bears) {
            JsonObject result = new JsonObject();
            result.addProperty("slug", inspector.getRepoSlug());

            Build build = inspector.getBuild();
            result.addProperty("buildId", build.getId());
            result.add("buildDate", serialize(build.getFinishedAt()));

            Build previousBuild = ((ProjectInspector4Bears) inspector).getPreviousBuild();
            if (previousBuild != null) {
                result.addProperty("previousBuildId", previousBuild.getId());
                result.add("previousBuildDate", serialize(previousBuild.getFinishedAt()));
            }

            result.addProperty("localRepo", inspector.getRepoLocalPath());

            this.inspectors.get(inspector.getState().name()).add(result);
        }
    }

    public void createOutput() throws IOException {
        this.dateFinish = new Date();

        JsonObject root = new JsonObject();

        root.add("dateStart", serialize(this.dateStart));
        root.add("dateFinish", serialize(this.dateFinish));
        root.add("scanner", serialize(this.scanner));

        JsonObject build_stats = new JsonObject();
        for (ProjectState state : ProjectState.values()) {
            build_stats.addProperty(state.name(), this.inspectors.get(state.name()).size());
        }

        root.add("build_stats", build_stats);
        root.add("builds", serialize(this.inspectors));

        writeFile(root);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        switch (inspector.getState()) {
            default:
                this.logger.warn("Project inspector with not defined state: " + inspector);
                break;

            case INIT:
                outputNotClonableInspector(inspector);
                break;

            case CLONABLE:
            case BUILDCHECKEDOUT:
                outputNotBuildableInspector(inspector);
                break;

            case BUILDNOTCHECKEDOUT:
            case NOTTESTABLE:
            case BUILDABLE:
                outputNotTestableInspector(inspector);
                break;

            case TESTABLE:
                outputFailWhenGatheringInfoInspector(inspector);
                break;

            case HASTESTERRORS:
            case HASTESTFAILURE:
            case CLASSPATHCOMPUTED:
            case SOURCEDIRCOMPUTED:
                outputHasTestFailureInspector(inspector);
                break;

            case NOTFAILING:
                outputNotFailingInspector(inspector);
                break;

            case PATCHED:
                outputHasBeenPatchedInspector(inspector);
                break;

            case DOESNOTHAVEPREVIOUSVERSION:
            case PREVIOUSVERSIONISNOTINTERESTING:
            case PREVIOUSBUILDCHECKEDOUT:
            case PREVIOUSBUILDNOTCHECKEDOUT:
            case PREVIOUSBUILDCODECHECKEDOUT:
            case PREVIOUSBUILDCODENOTCHECKEDOUT:
                outputBuildWithoutPreviousBuildInspector(inspector);
                break;

            case FIXERBUILD_CASE1:
            case FIXERBUILD_CASE2:
                outputFixerBuildInspector(inspector);
                break;
        }
    }
}
