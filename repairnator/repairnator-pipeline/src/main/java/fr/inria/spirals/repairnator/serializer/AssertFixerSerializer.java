package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.stamp.project.assertfixer.AssertFixerResult;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AssertFixerSerializer extends AbstractDataSerializer {

    public AssertFixerSerializer(List<SerializerEngine> engines) {
        super(engines, SerializerType.ASSERT_FIXER);
    }

    private List<Object> serializeAsList(ProjectInspector inspector, AssertFixerResult fixerResult) {
        BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();
        Build build = inspector.getBuggyBuild();

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId() + "");
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(Utils.getHostname());
        dataCol.add(Utils.formatCompleteDate(new Date()));
        dataCol.add(buildToBeInspected.getRunId());
        dataCol.add(fixerResult.getTestClass());
        dataCol.add(fixerResult.getTestMethod());
        dataCol.add(fixerResult.getDiff());
        dataCol.add(fixerResult.getExceptionMessage());

        return dataCol;
    }

    private JsonElement serializeAsJson(ProjectInspector inspector, AssertFixerResult fixerResult) {
        BuildToBeInspected buildToBeInspected = inspector.getBuildToBeInspected();
        Build build = inspector.getBuggyBuild();


        JsonObject result = new JsonObject();
        result.addProperty("buildId", build.getId());
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("hostname", Utils.getHostname());
        result.addProperty("fixerDateStr", Utils.formatCompleteDate(new Date()));
        this.addDate(result, "fixerDate", new Date());
        result.addProperty("runId", buildToBeInspected.getRunId());
        result.addProperty("testClassName", fixerResult.getTestClass());
        result.addProperty("testMethodName", fixerResult.getTestMethod());
        result.addProperty("diff", fixerResult.getDiff());
        result.addProperty("exception", fixerResult.getExceptionMessage());

        return result;
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        List<SerializedData> allData = new ArrayList<>();
        JobStatus jobStatus = inspector.getJobStatus();
        List<AssertFixerResult> fixerResultList = jobStatus.getAssertFixerResults();

        for (AssertFixerResult assertFixerResult : fixerResultList) {
            SerializedData data = new SerializedData(this.serializeAsList(inspector, assertFixerResult), this.serializeAsJson(inspector, assertFixerResult));
            allData.add(data);
        }

        for (SerializerEngine engine : this.getEngines()) {
            engine.serialize(allData, this.getType());
        }
    }
}
