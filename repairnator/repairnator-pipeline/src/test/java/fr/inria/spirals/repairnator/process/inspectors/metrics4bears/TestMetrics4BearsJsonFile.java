package fr.inria.spirals.repairnator.process.inspectors.metrics4bears;

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
import fr.inria.spirals.repairnator.Utils;
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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestMetrics4BearsJsonFile {

    private File tmpDir;

    private JsonSchema jsonSchema;
    private ObjectMapper jsonMapper;

    /* This list holds some properties from the json file that contain non-constant values as the date of the reproduction,
        thus, when checking the correctness of the properties' values, these properties are ignored.
     */
    private List<String> propertiesToIgnore;

    @Before
    public void setUp() throws IOException, ProcessingException {
        Utils.setLoggersLevel(Level.ERROR);

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setPush(true);
        config.setPushRemoteRepo("");
        config.setZ3solverPath(Utils4Tests.getZ3SolverPath());

        jsonMapper = new ObjectMapper();
        String workingDir = System.getProperty("user.dir");
        workingDir = workingDir.substring(0, workingDir.lastIndexOf("repairnator/"));
        String jsonSchemaFilePath = workingDir + "resources/bears-schema.json";
        File jsonSchemaFile = new File(jsonSchemaFilePath);
        JsonNode schemaObject = jsonMapper.readTree(jsonSchemaFile);

        LoadingConfiguration loadingConfiguration = LoadingConfiguration.newBuilder().dereferencing(Dereferencing.INLINE).freeze();
        JsonSchemaFactory factory = JsonSchemaFactory.newBuilder().setLoadingConfiguration(loadingConfiguration).freeze();

        jsonSchema = factory.getJsonSchema(schemaObject);

        propertiesToIgnore = new ArrayList<>();
        propertiesToIgnore.add("reproductionBuggyBuild.reproductionDateBeginning");
        propertiesToIgnore.add("reproductionBuggyBuild.reproductionDateEnd");
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
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
        FileHelper.deleteFile(tmpDir);
    }

    @Test
    public void testBearsJsonFileWithPassingPassingBuilds() throws IOException, ProcessingException {
        long buggyBuildCandidateId = 386337343; // https://travis-ci.org/fermadeiral/test-repairnator-bears/builds/386337343
        long patchedBuildCandidateId = 386348522; // https://travis-ci.org/fermadeiral/test-repairnator-bears/builds/386348522

        tmpDir = Files.createTempDirectory("test_bears_json_file_passing_passing_builds").toFile();

        Build buggyBuildCandidate = this.checkBuildAndReturn(buggyBuildCandidateId, false);
        Build patchedBuildCandidate = this.checkBuildAndReturn(patchedBuildCandidateId, false);

        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(buggyBuildCandidate, patchedBuildCandidate, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, "test");

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.BEARS);

        ProjectInspector4Bears inspector = new ProjectInspector4Bears(buildToBeInspected, tmpDir.getAbsolutePath(), null, null);
        inspector.run();

        // check bears.json against schema

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
            if (fieldComparisonFailureName.equals("tests.failingModule")) {
                String path = "fermadeiral/test-repairnator-bears/386337343";
                String expected = (String) fieldComparisonFailure.getExpected();
                expected = expected.substring(expected.indexOf(path), expected.length());
                String actual = (String) fieldComparisonFailure.getActual();
                actual = actual.substring(actual.indexOf(path), actual.length());
                assertTrue("Property failing: " + fieldComparisonFailureName,
                        actual.equals(expected));
            } else {
                assertTrue("Property failing: " + fieldComparisonFailureName,
                        this.isPropertyToBeIgnored(fieldComparisonFailureName));
            }
        }
    }

    @Test
    public void testBearsJsonFileWithFailingBuild() throws IOException, ProcessingException {
        long buggyBuildCandidateId = 208897371; // https://travis-ci.org/surli/failingProject/builds/208897371

        tmpDir = Files.createTempDirectory("test_bears_json_file_failing_build").toFile();

        Build buggyBuildCandidate = this.checkBuildAndReturn(buggyBuildCandidateId, false);

        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(buggyBuildCandidate, null, ScannedBuildStatus.ONLY_FAIL, "test");

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);
        config.setRepairTools(new HashSet<>(Arrays.asList("Nopol")));

        ProjectInspector inspector = new ProjectInspector(buildToBeInspected, tmpDir.getAbsolutePath(), null, null);
        inspector.run();

        // FIXME: check bears.json against schema: this fails since the bears schema was designed for Bears. Errors:
        // - "type" does not accept "only_fail"
        // - "builds/fixerBuild" is required
        // - "commits/fixerBuild" is required

        /*JsonNode bearsJsonFile = jsonMapper.readTree(new File(inspector.getRepoToPushLocalPath() + "/bears.json"));

        ProcessingReport report = jsonSchema.validate(bearsJsonFile);

        String message = "";
        for (ProcessingMessage processingMessage : report) {
            message += processingMessage.toString()+"\n";
        }
        assertTrue(message, report.isSuccess());*/

        // check correctness of the properties

        File expectedFile = new File(TestMetrics4BearsJsonFile.class.getResource("/json-files/bears-208897371.json").getPath());
        String expectedString = FileUtils.readFileToString(expectedFile, StandardCharsets.UTF_8);

        File actualFile = new File(inspector.getRepoToPushLocalPath() + "/bears.json");
        String actualString = FileUtils.readFileToString(actualFile, StandardCharsets.UTF_8);

        JSONCompareResult result = JSONCompare.compareJSON(expectedString, actualString, JSONCompareMode.STRICT);
        assertThat(result.isMissingOnField(), is(false));
        assertThat(result.isUnexpectedOnField(), is(false));

        for (FieldComparisonFailure fieldComparisonFailure : result.getFieldFailures()) {
            String fieldComparisonFailureName = fieldComparisonFailure.getField();
            if (fieldComparisonFailureName.equals("tests.failingModule")) {
                String path = "surli/failingProject/208897371";
                String expected = (String) fieldComparisonFailure.getExpected();
                expected = expected.substring(expected.indexOf(path), expected.length());
                String actual = (String) fieldComparisonFailure.getActual();
                actual = actual.substring(actual.indexOf(path), actual.length());
                assertTrue("Property failing: " + fieldComparisonFailureName,
                        actual.equals(expected));
            } else {
                assertTrue("Property failing: " + fieldComparisonFailureName,
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
