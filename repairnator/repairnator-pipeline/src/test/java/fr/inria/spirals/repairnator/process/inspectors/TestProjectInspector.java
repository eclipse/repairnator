package fr.inria.spirals.repairnator.process.inspectors;

import ch.qos.logback.classic.Level;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.states.PushState;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer;
import fr.inria.spirals.repairnator.serializer.NopolSerializer;
import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
        int buildId = 208897371; // surli/failingProject only-one-failing

        Path tmpDirPath = Files.createTempDirectory("test_complete");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        Build failingBuild = BuildHelper.getBuildFromId(buildId, null);

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
        serializers.add(new NopolSerializer(serializerEngines));

        notifiers.add(new PatchNotifier(notifierEngines));

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);

        ProjectInspector inspector = new ProjectInspector(buildToBeInspected, tmpDir.getAbsolutePath(), serializers, notifiers);
        inspector.run();

        JobStatus jobStatus = inspector.getJobStatus();
        assertThat(jobStatus.getPipelineState(), is(PipelineState.PATCHED));
        assertThat(jobStatus.getPushState(), is(PushState.REPAIR_INFO_COMMITTED));
        assertThat(jobStatus.getFailureLocations().size(), is(1));
        assertThat(jobStatus.getMetrics().getFailureNames().size(), is(1));

        verify(notifierEngine, times(1)).notify(anyString(), anyString());
        verify(serializerEngine, times(1)).serialize(anyListOf(SerializedData.class), eq(SerializerType.INSPECTOR));
        verify(serializerEngine, times(1)).serialize(anyListOf(SerializedData.class), eq(SerializerType.NOPOL));

        Git gitDir = Git.open(new File(inspector.getRepoToPushLocalPath()));
        Iterable<RevCommit> logs = gitDir.log().call();

        Iterator<RevCommit> iterator = logs.iterator();
        assertThat(iterator.hasNext(), is(true));

        RevCommit commit = iterator.next();
        assertThat(commit.getShortMessage(), containsString("End of the repairnator process"));

        commit = iterator.next();
        assertThat(commit.getShortMessage(), containsString("Automated patch"));

        commit = iterator.next();
        assertThat(commit.getShortMessage(), containsString("Bug commit."));

        assertThat(iterator.hasNext(), is(false));
    }
}
