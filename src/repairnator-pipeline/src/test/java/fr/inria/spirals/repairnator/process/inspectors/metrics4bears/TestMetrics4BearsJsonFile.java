package fr.inria.spirals.repairnator.process.inspectors.metrics4bears;

import fr.inria.spirals.repairnator.process.inspectors.InspectorFactory;
import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.Dereferencing;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.process.utils4tests.Utils4Tests;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.FieldComparisonFailure;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

public class TestMetrics4BearsJsonFile {

    private File tmpDir;

    /* This list holds some properties from the json file that contain non-constant values as the date of the reproduction,
        thus, when checking the correctness of the properties' values, these properties are ignored.
     */
    private List<String> propertiesToIgnore;

    @Before
    public void setUp() {
        Utils.setLoggersLevel(Level.ERROR);

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setPush(true);
        config.setPushRemoteRepo("");
        config.setZ3solverPath(Utils4Tests.getZ3SolverPath());
        config.setGithubUserEmail("noreply@github.com");
        config.setGithubUserName("repairnator");
        config.setJTravisEndpoint("https://api.travis-ci.com");

        propertiesToIgnore = new ArrayList<>();
        propertiesToIgnore.add("reproductionBuggyBuild.reproductionDateBeginning");
        propertiesToIgnore.add("reproductionBuggyBuild.reproductionDateEnd");
        propertiesToIgnore.add("reproductionBuggyBuild.machineInfo.hostName");
        propertiesToIgnore.add("reproductionBuggyBuild.machineInfo.numberCPU");
        propertiesToIgnore.add("reproductionBuggyBuild.machineInfo.freeMemory");
        propertiesToIgnore.add("reproductionBuggyBuild.machineInfo.totalMemory");
        propertiesToIgnore.add("reproductionBuggyBuild.totalDuration");
        propertiesToIgnore.add("reproductionBuggyBuild.processDurations.cloning.stepDurations");
        propertiesToIgnore.add("reproductionBuggyBuild.processDurations.cloning.totalDuration");
        propertiesToIgnore.add("reproductionBuggyBuild.processDurations.building.stepDurations");
        propertiesToIgnore.add("reproductionBuggyBuild.processDurations.building.totalDuration");
        propertiesToIgnore.add("reproductionBuggyBuild.processDurations.testing.stepDurations");
        propertiesToIgnore.add("reproductionBuggyBuild.processDurations.testing.totalDuration");
        propertiesToIgnore.add("reproductionBuggyBuild.processDurations.fixing.stepDurations");
        propertiesToIgnore.add("reproductionBuggyBuild.processDurations.fixing.totalDuration");
        propertiesToIgnore.add("builds.buggyBuild.date");
        propertiesToIgnore.add("builds.fixerBuild.date");
        propertiesToIgnore.add("commits.buggyBuild.date");
        propertiesToIgnore.add("commits.fixerBuild.date");

        // FIXME: the following property should not be ignored.
        // Locally, when running this test class with cloc installed, the tests pass just fine.
        // However, in Travis, the tests fail because such property is 0 in both tests.
        propertiesToIgnore.add("projectMetrics.numberLines");
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
        FileHelper.deleteFile(tmpDir);
    }

    @Test
    public void testBearsJsonFileWithPassingPassingBuilds() throws IOException, ProcessingException {
        long buggyBuildCandidateId = 225920540; // https://travis-ci.com/github/repairnator/test-repairnator-bears/builds/225920540
        long patchedBuildCandidateId = 225920529; // https://travis-ci.com/github/repairnator/test-repairnator-bears/builds/225920529

        tmpDir = Files.createTempDirectory("test_bears_json_file_passing_passing_builds").toFile();

        Build buggyBuildCandidate = this.checkBuildAndReturn(buggyBuildCandidateId, false);
        Build patchedBuildCandidate = this.checkBuildAndReturn(patchedBuildCandidateId, false);

        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(buggyBuildCandidate, patchedBuildCandidate, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, "test");

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.BEARS);

        ProjectInspector4Bears inspector = (ProjectInspector4Bears)InspectorFactory.getBearsInspector(buildToBeInspected, tmpDir.getAbsolutePath(), null);
        inspector.run();

        // check bears.json against schema

        ObjectMapper jsonMapper = new ObjectMapper();
        String workingDir = System.getProperty("user.dir");
        // In CI the base dir has a diferent name, and it changes with the PR
        if (workingDir.contains("repairnator/")) {
            workingDir = workingDir.substring(0, workingDir.lastIndexOf("repairnator/") + "repairnator/".length());
        } else {
            // We use the stable part (i.e. the sub-module) to get the workingDir
            workingDir = workingDir.substring(0, workingDir.lastIndexOf("src/repairnator-pipeline"));
        }
        String jsonSchemaFilePath = workingDir + "/resources/bears-schema.json";
        File jsonSchemaFile = new File(jsonSchemaFilePath);
        JsonNode schemaObject = jsonMapper.readTree(jsonSchemaFile);

        LoadingConfiguration loadingConfiguration = LoadingConfiguration.newBuilder().dereferencing(Dereferencing.INLINE).freeze();
        JsonSchemaFactory factory = JsonSchemaFactory.newBuilder().setLoadingConfiguration(loadingConfiguration).freeze();

        JsonSchema jsonSchema = factory.getJsonSchema(schemaObject);

        JsonNode bearsJsonFile = jsonMapper.readTree(new File(inspector.getRepoToPushLocalPath() + "/bears.json"));

        ProcessingReport report = jsonSchema.validate(bearsJsonFile);

        String message = "";
        for (ProcessingMessage processingMessage : report) {
            message += processingMessage.toString()+"\n";
        }
        assertTrue(message, report.isSuccess());

        // check correctness of the properties

        File expectedFile = new File(TestMetrics4BearsJsonFile.class.getResource("/json-files/bears-386337343-386348522.json").getPath());
        String expectedString = FileUtils.readFileToString(expectedFile, StandardCharsets.UTF_8);

        File actualFile = new File(inspector.getRepoToPushLocalPath() + "/bears.json");
        String actualString = FileUtils.readFileToString(actualFile, StandardCharsets.UTF_8);

        JSONCompareResult result = JSONCompare.compareJSON(expectedString, actualString, JSONCompareMode.STRICT);
        assertThat(result.isMissingOnField(), is(false));
        assertThat(result.isUnexpectedOnField(), is(false));

        for (FieldComparisonFailure fieldComparisonFailure : result.getFieldFailures()) {
            String fieldComparisonFailureName = fieldComparisonFailure.getField();
            if (fieldComparisonFailureName.equals("tests.failingModule") ||
                    fieldComparisonFailureName.equals("reproductionBuggyBuild.projectRootPomPath")) {
                String path = "repairnator/test-repairnator-bears";
                String expected = (String) fieldComparisonFailure.getExpected();
                expected = expected.substring(expected.indexOf(path), expected.length());
                String actual = (String) fieldComparisonFailure.getActual();
                actual = actual.substring(actual.indexOf(path), actual.length());
                assertTrue("Property failing: " + fieldComparisonFailureName,
                        actual.equals(expected));
            } else {
                assertTrue("Property failing: " + fieldComparisonFailureName +
                                "\nexpected: " + fieldComparisonFailure.getExpected() +
                                "\nactual: " + fieldComparisonFailure.getActual(),
                        this.isPropertyToBeIgnored(fieldComparisonFailureName));
            }
        }
    }

    // FIXME: This test is working locally, but there is an issue with the repoToPushLocalPath when running in CI.
    // As far as I understood, it is using the repoToPushLocalPath from another test (TestProjectInspector#testPatchFailingProject)
    // but wasn't able to fix it.
    @Ignore
    @Test
    public void testRepairnatorJsonFileWithFailingBuild() throws IOException, ProcessingException {
        long buggyBuildCandidateId = 220944190; // https://travis-ci.com/github/repairnator/failingProject/builds/220944190

        tmpDir = Files.createTempDirectory("test_repairnator_json_file_failing_build").toFile();

        Build buggyBuildCandidate = this.checkBuildAndReturn(buggyBuildCandidateId, false);

        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(buggyBuildCandidate, null, ScannedBuildStatus.ONLY_FAIL, "test");

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);
        config.setRepairTools(new HashSet<>(Arrays.asList("NopolSingleTest")));

        ProjectInspector inspector = InspectorFactory.getTravisInspector(buildToBeInspected, tmpDir.getAbsolutePath(), null);
        inspector.run();

        // check repairnator.json against schema

        ObjectMapper jsonMapper = new ObjectMapper();
        String workingDir = System.getProperty("user.dir");
        // In CI the base dir has a diferent name, and it changes with the PR
        if (workingDir.contains("repairnator/")) {
            workingDir = workingDir.substring(0, workingDir.lastIndexOf("repairnator/") + "repairnator/".length());
        } else {
            // We use the stable part (i.e. the sub-module) to get the workingDir
            workingDir = workingDir.substring(0, workingDir.lastIndexOf("src/repairnator-pipeline"));
        }
        String jsonSchemaFilePath = workingDir + "/resources/repairnator-schema.json";
        File jsonSchemaFile = new File(jsonSchemaFilePath);
        JsonNode schemaObject = jsonMapper.readTree(jsonSchemaFile);

        LoadingConfiguration loadingConfiguration = LoadingConfiguration.newBuilder().dereferencing(Dereferencing.INLINE).freeze();
        JsonSchemaFactory factory = JsonSchemaFactory.newBuilder().setLoadingConfiguration(loadingConfiguration).freeze();

        JsonSchema jsonSchema = factory.getJsonSchema(schemaObject);

        JsonNode repairnatorJsonFile = jsonMapper.readTree(new File(inspector.getRepoToPushLocalPath() + "/repairnator.json"));

        ProcessingReport report = jsonSchema.validate(repairnatorJsonFile);

        String message = "";
        for (ProcessingMessage processingMessage : report) {
            message += processingMessage.toString()+"\n";
        }
        assertTrue(message, report.isSuccess());

        // check correctness of the properties

        File expectedFile = new File(TestMetrics4BearsJsonFile.class.getResource("/json-files/repairnator-208897371.json").getPath());
        String expectedString = FileUtils.readFileToString(expectedFile, StandardCharsets.UTF_8);

        File actualFile = new File(inspector.getRepoToPushLocalPath() + "/repairnator.json");
        String actualString = FileUtils.readFileToString(actualFile, StandardCharsets.UTF_8);

        JSONCompareResult result = JSONCompare.compareJSON(expectedString, actualString, JSONCompareMode.STRICT);
        assertThat(result.isMissingOnField(), is(false));
        assertThat(result.isUnexpectedOnField(), is(false));

        for (FieldComparisonFailure fieldComparisonFailure : result.getFieldFailures()) {
            String fieldComparisonFailureName = fieldComparisonFailure.getField();
            if (fieldComparisonFailureName.equals("tests.failureDetails[0].detail")) {
                String expected = (String) fieldComparisonFailure.getExpected();
                String actual = (String) fieldComparisonFailure.getActual();
                assertTrue("Property failing: " + fieldComparisonFailureName,
                        actual.replaceAll("\\s+", "")
                                .equals(expected.replaceAll("\\s+", "")));
            } else if (fieldComparisonFailureName.equals("tests.failingModule") ||
                    fieldComparisonFailureName.equals("reproductionBuggyBuild.projectRootPomPath")) {
                String path = "repairnator/failingProject";
                String expected = (String) fieldComparisonFailure.getExpected();
                expected = expected.substring(expected.indexOf(path), expected.length());
                String actual = (String) fieldComparisonFailure.getActual();
                actual = actual.substring(actual.indexOf(path), actual.length());
                assertTrue("Property failing: " + fieldComparisonFailureName,
                        actual.equals(expected));
            } else {
                assertTrue("Property failing: " + fieldComparisonFailureName +
                                "\nexpected: " + fieldComparisonFailure.getExpected() +
                                "\nactual: " + fieldComparisonFailure.getActual(),
                        this.isPropertyToBeIgnored(fieldComparisonFailureName));
            }
        }
    }

    private Build checkBuildAndReturn(long buildId, boolean isPR) {
        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());

        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));
        assertThat(build.isPullRequest(), is(isPR));

        return build;
    }

    private boolean isPropertyToBeIgnored(String propertyName) {
        boolean isToIgnore = false;
        int i = 0;
        String fieldToIgnore;
        while (i < propertiesToIgnore.size() && !isToIgnore) {
            fieldToIgnore = propertiesToIgnore.get(i);
            if (propertyName.startsWith(fieldToIgnore)) {
                isToIgnore = true;
            }
            i++;
        }
        return isToIgnore;
    }

}
