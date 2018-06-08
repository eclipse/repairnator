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
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.builds.Builds;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.commits.Commits;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.patchDiff.PatchDiff;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.projectMetrics.ProjectMetrics;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.repository.Repository;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.reproductionBuggyBuild.GlobalStepInfo;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.reproductionBuggyBuild.ReproductionBuggyBuild;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.tests.FailingClass;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.tests.Failure;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.tests.FailureDetail;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.tests.Tests;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestMetrics4BearsJsonFile {

    private static final String SOLVER_PATH_DIR = "src/test/resources/z3/";
    private static final String SOLVER_NAME_LINUX = "z3_for_linux";
    private static final String SOLVER_NAME_MAC = "z3_for_mac";

    private File tmpDir;

    private JsonSchema jsonSchema;
    private ObjectMapper jsonMapper;

    @Before
    public void setUp() throws IOException, ProcessingException {
        Utils.setLoggersLevel(Level.ERROR);

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setPush(true);
        config.setPushRemoteRepo("");

        String solverPath;
        if (isMac()) {
            solverPath = SOLVER_PATH_DIR+SOLVER_NAME_MAC;
        } else {
            solverPath = SOLVER_PATH_DIR+SOLVER_NAME_LINUX;
        }
        config.setZ3solverPath(solverPath);

        jsonMapper = new ObjectMapper();
        String workingDir = System.getProperty("user.dir");
        workingDir = workingDir.substring(0, workingDir.lastIndexOf("repairnator/"));
        String jsonSchemaFilePath = workingDir + "resources/bears-schema.json";
        File jsonSchemaFile = new File(jsonSchemaFilePath);
        JsonNode schemaObject = jsonMapper.readTree(jsonSchemaFile);

        LoadingConfiguration loadingConfiguration = LoadingConfiguration.newBuilder().dereferencing(Dereferencing.INLINE).freeze();
        JsonSchemaFactory factory = JsonSchemaFactory.newBuilder().setLoadingConfiguration(loadingConfiguration).freeze();

        jsonSchema = factory.getJsonSchema(schemaObject);
    }

    public static boolean isMac() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.contains("mac"));
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
        GitHelper.deleteFile(tmpDir);
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

        Metrics4Bears metrics4Bears = inspector.getJobStatus().getMetrics4Bears();
        assertThat(metrics4Bears.getVersion(), notNullValue());
        assertThat(metrics4Bears.getType(), is("passing_passing"));

        Repository repository = metrics4Bears.getRepository();
        assertThat(repository.getName(), is("fermadeiral/test-repairnator-bears"));
        // FIXME: the returned github id from jTravis is different from the one I think it's correct, this should be checked
        //assertThat(repository.getGithubId(), is(135598437));
        assertThat(repository.getUrl(), is("https://github.com/fermadeiral/test-repairnator-bears"));
        assertThat(repository.getIsFork(), is(false));
        assertThat(repository.getOriginal().getName(), is(""));
        assertThat(repository.getOriginal().getGithubId(), is(0L));
        assertThat(repository.getOriginal().getUrl(), is(""));
        assertThat(repository.getIsPullRequest(), is(false));
        assertThat(repository.getPullRequestId(), is (0));

        Builds builds = metrics4Bears.getBuilds();
        assertThat(builds.getBuggyBuild().getId(), is(386337343L));
        assertThat(builds.getBuggyBuild().getUrl(), is("http://travis-ci.org/fermadeiral/test-repairnator-bears/builds/386337343"));

        assertThat(builds.getFixerBuild().getId(), is(386348522L));
        assertThat(builds.getFixerBuild().getUrl(), is("http://travis-ci.org/fermadeiral/test-repairnator-bears/builds/386348522"));

        Commits commits = metrics4Bears.getCommits();
        assertThat(commits.getBuggyBuild().getRepoName(), is("fermadeiral/test-repairnator-bears"));
        assertThat(commits.getBuggyBuild().getBranchName(), is("master"));
        assertThat(commits.getBuggyBuild().getSha(), is("bfdf6af10937db8ecde7a060b55d18864663abd5"));
        assertThat(commits.getBuggyBuild().getUrl(), is("http://github.com/fermadeiral/test-repairnator-bears/commit/bfdf6af10937db8ecde7a060b55d18864663abd5"));

        assertThat(commits.getFixerBuild().getRepoName(), is("fermadeiral/test-repairnator-bears"));
        assertThat(commits.getFixerBuild().getBranchName(), is("master"));
        assertThat(commits.getFixerBuild().getSha(), is("5b2ed0064d4c5e0fade39125cc071bd6593df869"));
        assertThat(commits.getFixerBuild().getUrl(), is("http://github.com/fermadeiral/test-repairnator-bears/commit/5b2ed0064d4c5e0fade39125cc071bd6593df869"));

        assertThat(commits.getBuggyBuildForkRepo(), nullValue());
        assertThat(commits.getBuggyBuildBaseRepo(), nullValue());
        assertThat(commits.getFixerBuildForkRepo(), nullValue());
        assertThat(commits.getFixerBuildBaseRepo(), nullValue());

        Tests tests = metrics4Bears.getTests();
        assertTrue(tests.getFailingModule().endsWith("fermadeiral/test-repairnator-bears/386337343/test-repairnator-bears-patchstats"));
        assertThat(tests.getOverallMetrics().getNumberRunning(), is(9));
        assertThat(tests.getOverallMetrics().getNumberPassing(), is(8));
        assertThat(tests.getOverallMetrics().getNumberFailing(), is(1));
        assertThat(tests.getOverallMetrics().getNumberErroring(), is(0));
        assertThat(tests.getOverallMetrics().getNumberSkipping(), is(0));

        Set<Failure> failures = tests.getOverallMetrics().getFailures();
        assertThat(failures.size(), is(1));
        Iterator<Failure> failureIterator = failures.iterator();
        if (failureIterator.hasNext()) {
            Failure failure = failureIterator.next();
            assertThat(failure.getFailureName(), is("java.lang.AssertionError"));
            assertThat(failure.getIsError(), is(false));
            assertThat(failure.getOccurrences(), is(1));
        }

        Set<FailingClass> failingClasses = tests.getFailingClasses();
        assertThat(failingClasses.size(), is(1));
        Iterator<FailingClass> failingClassIterator = failingClasses.iterator();
        if (failingClassIterator.hasNext()) {
            FailingClass failingClass = failingClassIterator.next();
            assertThat(failingClass.getTestClass(), is("PatchStatsTest"));
            assertThat(failingClass.getNumberRunning(), is(4));
            assertThat(failingClass.getNumberPassing(), is(3));
            assertThat(failingClass.getNumberFailing(), is(1));
            assertThat(failingClass.getNumberErroring(), is(0));
            assertThat(failingClass.getNumberSkipping(), is(0));
        }

        Set<FailureDetail> failureDetails = tests.getFailureDetails();
        assertThat(failureDetails.size(), is(1));
        Iterator<FailureDetail> failureDetailIterator = failureDetails.iterator();
        if (failureDetailIterator.hasNext()) {
            FailureDetail failureDetail = failureDetailIterator.next();
            assertThat(failureDetail.getTestClass(), is("PatchStatsTest"));
            assertThat(failureDetail.getTestMethod(), is("testComputeFilesWithCommitThatRenameFile"));
            assertThat(failureDetail.getFailureName(), is("java.lang.AssertionError"));
            assertTrue(!failureDetail.getDetail().equals(""));
            assertThat(failureDetail.getIsError(), is(false));
        }

        PatchDiff patchDiff = metrics4Bears.getPatchDiff();
        assertThat(patchDiff.getFiles().getNumberAdded(), is(0));
        assertThat(patchDiff.getFiles().getNumberChanged(), is(1));
        assertThat(patchDiff.getFiles().getNumberDeleted(), is(0));
        assertThat(patchDiff.getLines().getNumberAdded(), is(15));
        assertThat(patchDiff.getLines().getNumberDeleted(), is(8));

        // FIXME: the expected values need to be analyzed
        //ProjectMetrics projectMetrics = metrics4Bears.getProjectMetrics();
        //assertThat(projectMetrics.getNumberSourceFiles(), is());
        //assertThat(projectMetrics.getNumberTestFiles(), is());
        //assertThat(projectMetrics.getNumberLibraries(), is());

        ReproductionBuggyBuild reproductionBuggyBuild = metrics4Bears.getReproductionBuggyBuild();
        assertThat(reproductionBuggyBuild.getReproductionDateBeginning(), notNullValue());
        assertThat(reproductionBuggyBuild.getReproductionDateEnd(), notNullValue());

        GlobalStepInfo cloning = reproductionBuggyBuild.getProcessDurations().getCloning();
        List<String> expectedCloningStepNames = new ArrayList<>(Arrays.asList("CloneRepository"));
        List<String> cloningStepNames = cloning.getStepNames();
        assertThat(cloning.getNbSteps(), is(expectedCloningStepNames.size()));
        assertThat(cloning.getStepNames().size(), is(expectedCloningStepNames.size()));
        assertThat(cloning.getStepDurations().size(), is(expectedCloningStepNames.size()));
        assertThat(cloningStepNames.containsAll(expectedCloningStepNames), is(true));

        GlobalStepInfo building = reproductionBuggyBuild.getProcessDurations().getBuilding();
        List<String> expectedBuildingStepNames = new ArrayList<>(Arrays.asList("CheckoutPatchedBuildCandidate",
                "ComputeSourceDir", "ComputeTestDir", "CheckoutBuggyBuildCandidateSourceCode",
                "BuildProjectBuggyBuildCandidateSourceCode"));
        List<String> buildingStepNames = building.getStepNames();
        assertThat(building.getNbSteps(), is(expectedBuildingStepNames.size()));
        assertThat(building.getStepNames().size(), is(expectedBuildingStepNames.size()));
        assertThat(building.getStepDurations().size(), is(expectedBuildingStepNames.size()));
        assertThat(buildingStepNames.containsAll(expectedBuildingStepNames), is(true));

        GlobalStepInfo testing = reproductionBuggyBuild.getProcessDurations().getTesting();
        List<String> expectedTestingStepNames = new ArrayList<>(Arrays.asList("TestProjectBuggyBuildCandidateSourceCode"));
        List<String> testingStepNames = testing.getStepNames();
        assertThat(testing.getNbSteps(), is(expectedTestingStepNames.size()));
        assertThat(testing.getStepNames().size(), is(expectedTestingStepNames.size()));
        assertThat(testing.getStepDurations().size(), is(expectedTestingStepNames.size()));
        assertThat(testingStepNames.containsAll(expectedTestingStepNames), is(true));

        GlobalStepInfo fixing = reproductionBuggyBuild.getProcessDurations().getFixing();
        List<String> expectedFixingStepNames = new ArrayList<>();
        List<String> fixingStepNames = fixing.getStepNames();
        assertThat(fixing.getNbSteps(), is(expectedFixingStepNames.size()));
        assertThat(fixing.getStepNames().size(), is(expectedFixingStepNames.size()));
        assertThat(fixing.getStepDurations().size(), is(expectedFixingStepNames.size()));
        assertThat(fixingStepNames.containsAll(expectedFixingStepNames), is(true));
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

        Metrics4Bears metrics4Bears = inspector.getJobStatus().getMetrics4Bears();
        assertThat(metrics4Bears.getVersion(), nullValue());
        assertThat(metrics4Bears.getType(), is("only_fail"));

        Repository repository = metrics4Bears.getRepository();
        assertThat(repository.getName(), is("surli/failingProject"));
        // FIXME: the returned github id from jTravis is different from the one I think it's correct, this should be checked
        //assertThat(repository.getGithubId(), is(78415513));
        assertThat(repository.getUrl(), is("https://github.com/surli/failingProject"));
        assertThat(repository.getIsFork(), is(false));
        assertThat(repository.getOriginal().getName(), is(""));
        assertThat(repository.getOriginal().getGithubId(), is(0L));
        assertThat(repository.getOriginal().getUrl(), is(""));
        assertThat(repository.getIsPullRequest(), is(false));
        assertThat(repository.getPullRequestId(), is (0));

        Builds builds = metrics4Bears.getBuilds();
        assertThat(builds.getBuggyBuild().getId(), is(208897371L));
        assertThat(builds.getBuggyBuild().getUrl(), is("http://travis-ci.org/surli/failingProject/builds/208897371"));

        assertThat(builds.getFixerBuild(), nullValue());

        Commits commits = metrics4Bears.getCommits();
        assertThat(commits.getBuggyBuild().getRepoName(), is("surli/failingProject"));
        assertThat(commits.getBuggyBuild().getBranchName(), is("only-one-failing"));
        assertThat(commits.getBuggyBuild().getSha(), is("e17771af92490121d4b1655c0bdf36b3692f1ce3"));
        assertThat(commits.getBuggyBuild().getUrl(), is("http://github.com/surli/failingProject/commit/e17771af92490121d4b1655c0bdf36b3692f1ce3"));

        assertThat(commits.getBuggyBuildForkRepo(), nullValue());
        assertThat(commits.getBuggyBuildBaseRepo(), nullValue());
        assertThat(commits.getFixerBuild(), nullValue());
        assertThat(commits.getFixerBuildForkRepo(), nullValue());
        assertThat(commits.getFixerBuildBaseRepo(), nullValue());

        Tests tests = metrics4Bears.getTests();
        assertTrue(tests.getFailingModule().endsWith("surli/failingProject/208897371"));
        assertThat(tests.getOverallMetrics().getNumberRunning(), is(8));
        assertThat(tests.getOverallMetrics().getNumberPassing(), is(7));
        assertThat(tests.getOverallMetrics().getNumberFailing(), is(0));
        assertThat(tests.getOverallMetrics().getNumberErroring(), is(1));
        assertThat(tests.getOverallMetrics().getNumberSkipping(), is(0));

        Set<Failure> failures = tests.getOverallMetrics().getFailures();
        assertThat(failures.size(), is(1));
        Iterator<Failure> failureIterator = failures.iterator();
        if (failureIterator.hasNext()) {
            Failure failure = failureIterator.next();
            assertThat(failure.getFailureName(), is("java.lang.StringIndexOutOfBoundsException"));
            assertThat(failure.getIsError(), is(true));
            assertThat(failure.getOccurrences(), is(1));
        }

        Set<FailingClass> failingClasses = tests.getFailingClasses();
        assertThat(failingClasses.size(), is(1));
        Iterator<FailingClass> failingClassIterator = failingClasses.iterator();
        if (failingClassIterator.hasNext()) {
            FailingClass failingClass = failingClassIterator.next();
            assertThat(failingClass.getTestClass(), is("nopol_examples.nopol_example_1.NopolExampleTest"));
            assertThat(failingClass.getNumberRunning(), is(8));
            assertThat(failingClass.getNumberPassing(), is(7));
            assertThat(failingClass.getNumberFailing(), is(0));
            assertThat(failingClass.getNumberErroring(), is(1));
            assertThat(failingClass.getNumberSkipping(), is(0));
        }

        Set<FailureDetail> failureDetails = tests.getFailureDetails();
        assertThat(failureDetails.size(), is(1));
        Iterator<FailureDetail> failureDetailIterator = failureDetails.iterator();
        if (failureDetailIterator.hasNext()) {
            FailureDetail failureDetail = failureDetailIterator.next();
            assertThat(failureDetail.getTestClass(), is("nopol_examples.nopol_example_1.NopolExampleTest"));
            assertThat(failureDetail.getTestMethod(), is("test5"));
            assertThat(failureDetail.getFailureName(), is("java.lang.StringIndexOutOfBoundsException"));
            assertTrue(!failureDetail.getDetail().equals(""));
            assertThat(failureDetail.getIsError(), is(true));
        }

        PatchDiff patchDiff = metrics4Bears.getPatchDiff();
        assertThat(patchDiff.getFiles().getNumberAdded(), is(0));
        assertThat(patchDiff.getFiles().getNumberChanged(), is(0));
        assertThat(patchDiff.getFiles().getNumberDeleted(), is(0));
        assertThat(patchDiff.getLines().getNumberAdded(), is(0));
        assertThat(patchDiff.getLines().getNumberDeleted(), is(0));

        // FIXME: the expected values need to be analyzed
        //ProjectMetrics projectMetrics = metrics4Bears.getProjectMetrics();
        //assertThat(projectMetrics.getNumberSourceFiles(), is());
        //assertThat(projectMetrics.getNumberTestFiles(), is());
        //assertThat(projectMetrics.getNumberLibraries(), is());

        ReproductionBuggyBuild reproductionBuggyBuild = metrics4Bears.getReproductionBuggyBuild();
        assertThat(reproductionBuggyBuild.getReproductionDateBeginning(), notNullValue());
        assertThat(reproductionBuggyBuild.getReproductionDateEnd(), notNullValue());

        GlobalStepInfo cloning = reproductionBuggyBuild.getProcessDurations().getCloning();
        List<String> expectedCloningStepNames = new ArrayList<>(Arrays.asList("CloneRepository"));
        List<String> cloningStepNames = cloning.getStepNames();
        assertThat(cloning.getNbSteps(), is(expectedCloningStepNames.size()));
        assertThat(cloning.getStepNames().size(), is(expectedCloningStepNames.size()));
        assertThat(cloning.getStepDurations().size(), is(expectedCloningStepNames.size()));
        assertThat(cloningStepNames.containsAll(expectedCloningStepNames), is(true));

        GlobalStepInfo building = reproductionBuggyBuild.getProcessDurations().getBuilding();
        List<String> expectedBuildingStepNames = new ArrayList<>(Arrays.asList("CheckoutBuggyBuild", "BuildProject"));
        List<String> buildingStepNames = building.getStepNames();
        assertThat(building.getNbSteps(), is(expectedBuildingStepNames.size()));
        assertThat(building.getStepNames().size(), is(expectedBuildingStepNames.size()));
        assertThat(building.getStepDurations().size(), is(expectedBuildingStepNames.size()));
        assertThat(buildingStepNames.containsAll(expectedBuildingStepNames), is(true));

        GlobalStepInfo testing = reproductionBuggyBuild.getProcessDurations().getTesting();
        List<String> expectedTestingStepNames = new ArrayList<>(Arrays.asList("TestProject"));
        List<String> testingStepNames = testing.getStepNames();
        assertThat(testing.getNbSteps(), is(expectedTestingStepNames.size()));
        assertThat(testing.getStepNames().size(), is(expectedTestingStepNames.size()));
        assertThat(testing.getStepDurations().size(), is(expectedTestingStepNames.size()));
        assertThat(testingStepNames.containsAll(expectedTestingStepNames), is(true));

        GlobalStepInfo fixing = reproductionBuggyBuild.getProcessDurations().getFixing();
        List<String> expectedFixingStepNames = new ArrayList<>(Arrays.asList("Nopol"));
        List<String> fixingStepNames = fixing.getStepNames();
        assertThat(fixing.getNbSteps(), is(expectedFixingStepNames.size()));
        assertThat(fixing.getStepNames().size(), is(expectedFixingStepNames.size()));
        assertThat(fixing.getStepDurations().size(), is(expectedFixingStepNames.size()));
        assertThat(fixingStepNames.containsAll(expectedFixingStepNames), is(true));
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

}
