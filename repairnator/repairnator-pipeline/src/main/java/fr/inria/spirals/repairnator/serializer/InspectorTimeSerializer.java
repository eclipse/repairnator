package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.SquashRepository;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.NopolRepair;
import fr.inria.spirals.repairnator.process.step.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
        Map<String, Integer> durations = inspector.getJobStatus().getStepsDurationsInSeconds();

        int clonage = durations.getOrDefault(CloneRepository.class.getSimpleName(), 0);
        int checkoutBuild = durations.getOrDefault(CheckoutBuild.class.getSimpleName(), 0);
        int buildtime = durations.getOrDefault(BuildProject.class.getSimpleName(), 0);
        int test = durations.getOrDefault(TestProject.class.getSimpleName(), 0);
        int gatherTestInfo = durations.getOrDefault(GatherTestInformation.class.getSimpleName(), 0);
        int squashRepository = durations.getOrDefault(SquashRepository.class.getSimpleName(), 0);
        int push = durations.getOrDefault(PushIncriminatedBuild.class.getSimpleName(), 0);
        int computeClasspath = durations.getOrDefault(ComputeClasspath.class.getSimpleName(), 0);
        int computeSourceDir = durations.getOrDefault(ComputeSourceDir.class.getSimpleName(), 0);
        int repair = durations.getOrDefault(NopolRepair.class.getSimpleName(), 0);

        int totalDuration = clonage + checkoutBuild + buildtime + test + gatherTestInfo + squashRepository + push +
                computeClasspath + computeSourceDir + repair;

        Build build = inspector.getBuild();

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
        dataCol.add(squashRepository);
        dataCol.add(push);
        dataCol.add(computeClasspath);
        dataCol.add(computeSourceDir);
        dataCol.add(repair);
        dataCol.add(inspector.getBuildToBeInspected().getRunId());

        return dataCol;
    }

    private JsonElement serializeAsJson(ProjectInspector inspector) {
        Map<String, Integer> durations = inspector.getJobStatus().getStepsDurationsInSeconds();

        int clonage = durations.getOrDefault(CloneRepository.class.getSimpleName(), 0);
        int checkoutBuild = durations.getOrDefault(CheckoutBuild.class.getSimpleName(), 0);
        int buildtime = durations.getOrDefault(BuildProject.class.getSimpleName(), 0);
        int test = durations.getOrDefault(TestProject.class.getSimpleName(), 0);
        int gatherTestInfo = durations.getOrDefault(GatherTestInformation.class.getSimpleName(), 0);
        int squashRepository = durations.getOrDefault(SquashRepository.class.getSimpleName(), 0);
        int push = durations.getOrDefault(PushIncriminatedBuild.class.getSimpleName(), 0);
        int computeClasspath = durations.getOrDefault(ComputeClasspath.class.getSimpleName(), 0);
        int computeSourceDir = durations.getOrDefault(ComputeSourceDir.class.getSimpleName(), 0);
        int repair = durations.getOrDefault(NopolRepair.class.getSimpleName(), 0);

        int totalDuration = clonage + checkoutBuild + buildtime + test + gatherTestInfo + squashRepository + push +
                computeClasspath + computeSourceDir + repair;

        Build build = inspector.getBuild();

        JsonObject result = new JsonObject();

        result.addProperty("buildId", build.getId());
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("buildReproductionDate", Utils.formatCompleteDate(new Date()));
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("totalDuration", totalDuration);
        result.addProperty("clonage", clonage);
        result.addProperty("checkoutBuild", checkoutBuild);
        result.addProperty("build", buildtime);
        result.addProperty("test", test);
        result.addProperty("gatherTestInfo", gatherTestInfo);
        result.addProperty("squashRepository", squashRepository);
        result.addProperty("push", push);
        result.addProperty("computeClasspath", computeClasspath);
        result.addProperty("computeSourceDir", computeSourceDir);
        result.addProperty("repair", repair);
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
