package fr.inria.spirals.repairnator.process.inspectors;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.ResolveDependency;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.push.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.repair.AstorRepair;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer;
import fr.inria.spirals.repairnator.serializer.PatchesSerializer;
import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.ToolDiagnosticSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.states.PushState;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by urli on 24/04/2017.
 */
public class TestProjectInspector {

    private static final String SOLVER_PATH_DIR = "src/test/resources/z3/";
    private static final String SOLVER_NAME_LINUX = "z3_for_linux";
    private static final String SOLVER_NAME_MAC = "z3_for_mac";

    @Before
    public void setUp() {
        String solverPath;
        if (isMac()) {
            solverPath = SOLVER_PATH_DIR+SOLVER_NAME_MAC;
        } else {
            solverPath = SOLVER_PATH_DIR+SOLVER_NAME_LINUX;
        }

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setZ3solverPath(solverPath);
        config.setPush(true);
        config.setPushRemoteRepo("");
        config.setRepairTools(RepairToolsManager.getRepairToolsName());
        Utils.setLoggersLevel(Level.ERROR);
    }

    public static boolean isMac() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.contains("mac"));
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testPatchFailingProject() throws IOException, GitAPIException {
        long buildId = 208897371; // surli/failingProject only-one-failing

        Path tmpDirPath = Files.createTempDirectory("test_complete");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build failingBuild = optionalBuild.get();

        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(failingBuild, null, ScannedBuildStatus.ONLY_FAIL, "test");

        List<AbstractDataSerializer> serializers = new ArrayList<>();
        List<AbstractNotifier> notifiers = new ArrayList<>();

        List<SerializerEngine> serializerEngines = new ArrayList<>();
        SerializerEngine serializerEngine = mock(SerializerEngine.class);
        serializerEngines.add(serializerEngine);

        List<NotifierEngine> notifierEngines = new ArrayList<>();
        NotifierEngine notifierEngine = mock(NotifierEngine.class);
        notifierEngines.add(notifierEngine);

        serializers.add(new InspectorSerializer(serializerEngines));
        serializers.add(new PatchesSerializer(serializerEngines));
        serializers.add(new ToolDiagnosticSerializer(serializerEngines));

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);

        ProjectInspector inspector = new ProjectInspector(buildToBeInspected, tmpDir.getAbsolutePath(), serializers, notifiers);
        inspector.setPatchNotifier(new PatchNotifier(notifierEngines));
        inspector.run();

        JobStatus jobStatus = inspector.getJobStatus();

        List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();

        Map<Class<? extends AbstractStep>, StepStatus.StatusKind> expectedStatuses = new HashMap<>();
        expectedStatuses.put(AstorRepair.class, StepStatus.StatusKind.SKIPPED); // no patch found by Astor
        expectedStatuses.put(PushIncriminatedBuild.class, StepStatus.StatusKind.SKIPPED); // no remote info provided
        expectedStatuses.put(CheckoutPatchedBuild.class, StepStatus.StatusKind.FAILURE); // no patch build to find
        expectedStatuses.put(NPERepair.class, StepStatus.StatusKind.SKIPPED); // No NPE

        this.checkStepStatus(stepStatusList, expectedStatuses);

        assertThat(jobStatus.getPushState(), is(PushState.REPAIR_INFO_COMMITTED));
        assertThat(jobStatus.getFailureLocations().size(), is(1));
        assertThat(jobStatus.getMetrics().getFailureNames().size(), is(1));

        String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(finalStatus, is("PATCHED"));

        String remoteBranchName = "surli-failingProject-208897371-20170308-040702";
        assertEquals(remoteBranchName, inspector.getRemoteBranchName());

        verify(notifierEngine, times(1)).notify(anyString(), anyString());
        verify(serializerEngine, times(1)).serialize(anyListOf(SerializedData.class), eq(SerializerType.INSPECTOR));
        verify(serializerEngine, times(1)).serialize(anyListOf(SerializedData.class), eq(SerializerType.PATCHES));
        verify(serializerEngine, times(1)).serialize(anyListOf(SerializedData.class), eq(SerializerType.TOOL_DIAGNOSTIC));

        Git gitDir = Git.open(new File(inspector.getRepoToPushLocalPath()));
        Iterable<RevCommit> logs = gitDir.log().call();

        Iterator<RevCommit> iterator = logs.iterator();
        assertThat(iterator.hasNext(), is(true));

        RevCommit commit = iterator.next();
        assertThat(commit.getShortMessage(), containsString("End of the repairnator process"));

        commit = iterator.next();
        assertThat(commit.getShortMessage(), containsString("Automatic repair"));

        commit = iterator.next();
        assertThat(commit.getShortMessage(), containsString("Bug commit"));

        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testFailingProjectNotBuildable() throws IOException, GitAPIException {
        long buildId = 228303218; // surli/failingProject only-one-failing

        Path tmpDirPath = Files.createTempDirectory("test_complete2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build failingBuild = optionalBuild.get();

        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(failingBuild, null, ScannedBuildStatus.ONLY_FAIL, "test");

        List<AbstractDataSerializer> serializers = new ArrayList<>();
        List<AbstractNotifier> notifiers = new ArrayList<>();

        List<SerializerEngine> serializerEngines = new ArrayList<>();
        SerializerEngine serializerEngine = mock(SerializerEngine.class);
        serializerEngines.add(serializerEngine);

        serializers.add(new InspectorSerializer(serializerEngines));

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);

        ProjectInspector inspector = new ProjectInspector(buildToBeInspected, tmpDir.getAbsolutePath(), serializers, notifiers);
        inspector.run();

        JobStatus jobStatus = inspector.getJobStatus();
        assertThat(jobStatus.getPushState(), is(PushState.NONE));

        List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();

        for (int i = 0; i < stepStatusList.size(); i++) {
            StepStatus stepStatus = stepStatusList.get(i);
            if (i == stepStatusList.size() - 1) {
                assertThat(stepStatus.isSuccess(), is(false));
            } else {
                assertThat(stepStatus.isSuccess(), is(true));
            }
        }

        String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(finalStatus, is(PipelineState.NOTBUILDABLE.name()));

        verify(serializerEngine, times(1)).serialize(anyListOf(SerializedData.class), eq(SerializerType.INSPECTOR));

    }

    @Test
    public void testSpoonException() throws IOException {
        // one dependency missing: should not be buildable
        long buildId = 355743087; // ministryofjustice/laa-saml-mock

        Path tmpDirPath = Files.createTempDirectory("test_spoonexception");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build failingBuild = optionalBuild.get();

        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(failingBuild, null, ScannedBuildStatus.ONLY_FAIL, "test");

        List<AbstractDataSerializer> serializers = new ArrayList<>();
        List<AbstractNotifier> notifiers = new ArrayList<>();

        List<SerializerEngine> serializerEngines = new ArrayList<>();
        SerializerEngine serializerEngine = mock(SerializerEngine.class);
        serializerEngines.add(serializerEngine);

        serializers.add(new InspectorSerializer(serializerEngines));

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);

        ProjectInspector inspector = new ProjectInspector(buildToBeInspected, tmpDir.getAbsolutePath(), serializers, notifiers);
        inspector.run();

        JobStatus jobStatus = inspector.getJobStatus();

        List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();

        for (StepStatus stepStatus : stepStatusList) {
            if (stepStatus.getStep() instanceof ResolveDependency) {
                assertThat(stepStatus.isSuccess(), is(false));
            }
        }

        String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(finalStatus, is(PipelineState.NOTBUILDABLE.name()));

        verify(serializerEngine, times(1)).serialize(anyListOf(SerializedData.class), eq(SerializerType.INSPECTOR));

    }

    private void checkStepStatus(List<StepStatus> statuses, Map<Class<? extends AbstractStep>,StepStatus.StatusKind> expectedValues) {
        for (StepStatus stepStatus : statuses) {
            if (!expectedValues.containsKey(stepStatus.getStep().getClass())) {
                assertThat("Step failing: "+stepStatus, stepStatus.isSuccess(), is(true));
            } else {
                StepStatus.StatusKind expectedStatus = expectedValues.get(stepStatus.getStep().getClass());
                assertThat("Status was not as expected" + stepStatus, stepStatus.getStatus(), is(expectedStatus));
                expectedValues.remove(stepStatus.getStep().getClass());
            }
        }

        assertThat(expectedValues.isEmpty(), is(true));
    }
}
