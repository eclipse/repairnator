package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuildSourceCode;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by fernanda on 21/03/17.
 */
public class InspectorTimeSerializer4Bears extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(InspectorTimeSerializer4Bears.class);

    public InspectorTimeSerializer4Bears(List<SerializerEngine> engines) {
        super(engines, SerializerType.TIMES4BEARS);
    }

    private List<Object> serializeAsList(ProjectInspector inspector) {
        Map<String, Integer> durations = inspector.getJobStatus().getStepsDurationsInSeconds();

        int cloneRepository = durations.getOrDefault(CloneRepository.class.getSimpleName(), 0);
        int checkoutBuild = durations.getOrDefault(CheckoutPatchedBuild.class.getSimpleName(), 0);
        int buildProjectBuild = durations.getOrDefault(BuildProject.class.getSimpleName()+"Build", 0);
        int testProjectBuild = durations.getOrDefault(TestProject.class.getSimpleName()+"Build", 0);
        int gatherTestInformationBuild = durations.getOrDefault(GatherTestInformation.class.getSimpleName()+"Build", 0);
        int checkoutPreviousBuild = durations.getOrDefault(CheckoutBuggyBuild.class.getSimpleName(), 0);
        int buildProjectPreviousBuild = durations.getOrDefault(BuildProject.class.getSimpleName()+"PreviousBuild", 0);
        int testProjectPreviousBuild = durations.getOrDefault(TestProject.class.getSimpleName()+"PreviousBuild", 0);
        int gatherTestInformationPreviousBuild = durations.getOrDefault(GatherTestInformation.class.getSimpleName()+"PreviousBuild", 0);
        int checkoutPreviousBuildSourceCode = durations.getOrDefault(CheckoutBuggyBuildSourceCode.class.getSimpleName(), 0);
        int buildProjectPreviousBuildSourceCode = durations.getOrDefault(BuildProject.class.getSimpleName()+"PreviousBuildSourceCode", 0);
        int testProjectPreviousBuildSourceCode = durations.getOrDefault(TestProject.class.getSimpleName()+"PreviousBuildSourceCode", 0);
        int gatherTestInformationPreviousBuildSourceCode = durations.getOrDefault(GatherTestInformation.class.getSimpleName()+"PreviousBuildSourceCode", 0);
        int squashRepository = durations.getOrDefault(SquashRepository.class.getSimpleName(), 0);
        int pushBuild = durations.getOrDefault(PushIncriminatedBuild.class.getSimpleName(), 0);
        int computeClasspath = durations.getOrDefault(ComputeClasspath.class.getSimpleName(), 0);
        int dependencyResolution = durations.getOrDefault(ResolveDependency.class.getSimpleName(), 0);

        int totalDuration = cloneRepository + checkoutBuild + buildProjectBuild + testProjectBuild + gatherTestInformationBuild +
                checkoutPreviousBuild + buildProjectPreviousBuild + testProjectPreviousBuild + gatherTestInformationPreviousBuild +
                checkoutPreviousBuildSourceCode + buildProjectPreviousBuildSourceCode + testProjectPreviousBuildSourceCode +
                gatherTestInformationPreviousBuildSourceCode + squashRepository + pushBuild + computeClasspath + dependencyResolution;

        Build build = inspector.getPatchedBuild();

        Build previousBuild = inspector.getBuggyBuild();
        int previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId() + "");
        dataCol.add(previousBuildId + "");
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(Utils.getHostname());
        dataCol.add(totalDuration);
        dataCol.add(cloneRepository);
        dataCol.add(checkoutBuild);
        dataCol.add(buildProjectBuild);
        dataCol.add(testProjectBuild);
        dataCol.add(gatherTestInformationBuild);
        dataCol.add(checkoutPreviousBuild);
        dataCol.add(buildProjectPreviousBuild);
        dataCol.add(testProjectPreviousBuild);
        dataCol.add(gatherTestInformationPreviousBuild);
        dataCol.add(checkoutPreviousBuildSourceCode);
        dataCol.add(buildProjectPreviousBuildSourceCode);
        dataCol.add(testProjectPreviousBuildSourceCode);
        dataCol.add(gatherTestInformationPreviousBuildSourceCode);
        dataCol.add(squashRepository);
        dataCol.add(pushBuild);
        dataCol.add(computeClasspath);
        dataCol.add(dependencyResolution);

        return dataCol;
    }

    private JsonElement serializeAsJson(ProjectInspector inspector) {
        Map<String, Integer> durations = inspector.getJobStatus().getStepsDurationsInSeconds();

        int cloneRepository = durations.getOrDefault(CloneRepository.class.getSimpleName(), 0);
        int checkoutBuild = durations.getOrDefault(CheckoutPatchedBuild.class.getSimpleName(), 0);
        int buildProjectBuild = durations.getOrDefault(BuildProject.class.getSimpleName()+"Build", 0);
        int testProjectBuild = durations.getOrDefault(TestProject.class.getSimpleName()+"Build", 0);
        int gatherTestInformationBuild = durations.getOrDefault(GatherTestInformation.class.getSimpleName()+"Build", 0);
        int checkoutPreviousBuild = durations.getOrDefault(CheckoutBuggyBuild.class.getSimpleName(), 0);
        int buildProjectPreviousBuild = durations.getOrDefault(BuildProject.class.getSimpleName()+"PreviousBuild", 0);
        int testProjectPreviousBuild = durations.getOrDefault(TestProject.class.getSimpleName()+"PreviousBuild", 0);
        int gatherTestInformationPreviousBuild = durations.getOrDefault(GatherTestInformation.class.getSimpleName()+"PreviousBuild", 0);
        int checkoutPreviousBuildSourceCode = durations.getOrDefault(CheckoutBuggyBuildSourceCode.class.getSimpleName(), 0);
        int buildProjectPreviousBuildSourceCode = durations.getOrDefault(BuildProject.class.getSimpleName()+"PreviousBuildSourceCode", 0);
        int testProjectPreviousBuildSourceCode = durations.getOrDefault(TestProject.class.getSimpleName()+"PreviousBuildSourceCode", 0);
        int gatherTestInformationPreviousBuildSourceCode = durations.getOrDefault(GatherTestInformation.class.getSimpleName()+"PreviousBuildSourceCode", 0);
        int squashRepository = durations.getOrDefault(SquashRepository.class.getSimpleName(), 0);
        int pushBuild = durations.getOrDefault(PushIncriminatedBuild.class.getSimpleName(), 0);
        int computeClasspath = durations.getOrDefault(ComputeClasspath.class.getSimpleName(), 0);
        int dependencyResolution = durations.getOrDefault(ResolveDependency.class.getSimpleName(), 0);

        int totalDuration = cloneRepository + checkoutBuild + buildProjectBuild + testProjectBuild + gatherTestInformationBuild +
                checkoutPreviousBuild + buildProjectPreviousBuild + testProjectPreviousBuild + gatherTestInformationPreviousBuild +
                checkoutPreviousBuildSourceCode + buildProjectPreviousBuildSourceCode + testProjectPreviousBuildSourceCode +
                gatherTestInformationPreviousBuildSourceCode + squashRepository + pushBuild + dependencyResolution + computeClasspath;

        Build build = inspector.getPatchedBuild();

        Build previousBuild = inspector.getBuggyBuild();
        int previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;
        JsonObject result = new JsonObject();

        result.addProperty("buildId", build.getId());
        result.addProperty("previousBuildId", previousBuildId);
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("buildReproductionDateStr", Utils.formatCompleteDate(new Date()));
        this.addDate(result, "buildReproductionDate", new Date());

        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("totalDuration", totalDuration);
        result.addProperty("clonage", cloneRepository);
        result.addProperty("checkoutBuild", checkoutBuild);
        result.addProperty("build", buildProjectBuild);
        result.addProperty("test", testProjectBuild);
        result.addProperty("gatherTestInfo", gatherTestInformationBuild);
        result.addProperty("checkoutPrevious", checkoutPreviousBuild);
        result.addProperty("buildPrevious", buildProjectPreviousBuild);
        result.addProperty("testPrevious", testProjectPreviousBuild);
        result.addProperty("gatherTestInfoPrevious", gatherTestInformationPreviousBuild);
        result.addProperty("checkoutPreviousSourceCode", checkoutPreviousBuildSourceCode);
        result.addProperty("buildPreviousSourceCode", buildProjectPreviousBuildSourceCode);
        result.addProperty("testProjectPreviousBuildSourceCode", testProjectPreviousBuildSourceCode);
        result.addProperty("gatherTestInformationPreviousBuildSourceCode", gatherTestInformationPreviousBuildSourceCode);
        result.addProperty("squashRepository", squashRepository);
        result.addProperty("push", pushBuild);
        result.addProperty("dependendencyResolution", dependencyResolution);
        result.addProperty("computeClasspath", computeClasspath);
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
}
