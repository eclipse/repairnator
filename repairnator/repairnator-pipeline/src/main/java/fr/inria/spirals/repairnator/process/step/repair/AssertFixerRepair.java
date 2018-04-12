package fr.inria.spirals.repairnator.process.step.repair;

import com.google.gson.GsonBuilder;
import eu.stamp.project.assertfixer.AssertFixerResult;
import eu.stamp.project.assertfixer.Configuration;
import eu.stamp.project.assertfixer.Main;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AssertFixerRepair extends AbstractStep {
    private static final int TOTAL_TIME = 30; // 30 minutes

    public AssertFixerRepair(ProjectInspector inspector) {
        super(inspector);
    }

    public AssertFixerRepair(ProjectInspector inspector, String name) {
        super(inspector, name);
    }

    @Override
    protected void businessExecute() {
        this.getLogger().info("Start AssertFixerRepair");
        List<URL> classPath = this.inspector.getJobStatus().getRepairClassPath();
        File[] sources = this.inspector.getJobStatus().getRepairSourceDir();
        File[] tests = this.inspector.getJobStatus().getTestDir();

        if (tests == null || tests.length == 0) {
            addStepError("No test directory found, this step won't be executed.");
            return;
        }

        Configuration configuration = new Configuration();
        configuration.setVerbose(true);

        if (sources != null && sources.length > 0) {
            List<String> sourceList = new ArrayList<>();
            for (File source : sources) {
                sourceList.add(source.getAbsolutePath());
            }

            configuration.setPathToSourceFolder(sourceList);
        }

        List<String> testList = new ArrayList<>();
        for (String testFolder : testList) {
            testList.add(testFolder);
        }

        configuration.setPathToTestFolder(testList);

        StringBuilder classpathBuilder = new StringBuilder();
        for (int i = 0; i < classPath.size(); i++) {
            classpathBuilder.append(classPath.get(i).getPath());

            if (i < classPath.size() - 1) {
                classpathBuilder.append(":");
            }
        }
        configuration.setClasspath(classpathBuilder.toString());

        Map<String, List<String>> multipleTestCases = new HashMap<>();
        for (FailureLocation failureLocation : this.getInspector().getJobStatus().getFailureLocations()) {
            List<String> failingMethods = new ArrayList<>(failureLocation.getErroringMethods());
            failingMethods.addAll(failureLocation.getFailingMethods());
            multipleTestCases.put(failureLocation.getClassName(), failingMethods);
        }

        configuration.setMultipleTestCases(multipleTestCases);

        String asJson = new GsonBuilder().setPrettyPrinting().create().toJson(configuration);
        this.getLogger().info("Launcher AssertFixer with the following configuration: "+asJson);


        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<List<AssertFixerResult>> assertFixerExecution = executor.submit(() -> {
            try {
                Main main = new Main(configuration);
                return main.runWithResults();
            } catch (Throwable throwable) {
                addStepError("Got exception when running AssertFixer: ", throwable);
                return new ArrayList<>();
            }
        });

        List<AssertFixerResult> assertFixerResults = new ArrayList<>();
        try {
            executor.shutdown();
            assertFixerResults.addAll(assertFixerExecution.get(TOTAL_TIME, TimeUnit.MINUTES));
        } catch (Exception e) {
            addStepError("Error while executing AssertFixer", e);
        }

        this.getInspector().getJobStatus().setAssertFixerResults(assertFixerResults);
    }
}
