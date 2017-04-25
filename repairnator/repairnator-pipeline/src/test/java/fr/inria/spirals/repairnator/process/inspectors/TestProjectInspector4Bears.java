package fr.inria.spirals.repairnator.process.inspectors;

import ch.qos.logback.classic.Level;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.LauncherMode;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.notifier.FixerBuildNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
public class TestProjectInspector4Bears {

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testFailingPassingProject() throws IOException {
        int buildIdPassing = 203800961;
        int buildIdFailing = 203797975;


        Path tmpDirPath = Files.createTempDirectory("test_bears1");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        Build passingBuild = BuildHelper.getBuildFromId(buildIdPassing, null);
        Build failingBuild = BuildHelper.getBuildFromId(buildIdFailing, null);


        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(passingBuild, failingBuild, ScannedBuildStatus.FAILING_AND_PASSING, "test");

        List<AbstractDataSerializer> serializers = new ArrayList<>();
        List<AbstractNotifier> notifiers = new ArrayList<>();

        List<SerializerEngine> serializerEngines = new ArrayList<>();
        SerializerEngine serializerEngine = mock(SerializerEngine.class);
        serializerEngines.add(serializerEngine);

        List<NotifierEngine> notifierEngines = new ArrayList<>();
        NotifierEngine notifierEngine = mock(NotifierEngine.class);
        notifierEngines.add(notifierEngine);

        serializers.add(new InspectorSerializer4Bears(serializerEngines));

        notifiers.add(new FixerBuildNotifier(notifierEngines));

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.BEARS);

        ProjectInspector4Bears inspector = new ProjectInspector4Bears(buildToBeInspected, tmpDir.getAbsolutePath(), serializers, notifiers);
        inspector.run();

        JobStatus jobStatus = inspector.getJobStatus();
        assertThat(jobStatus.getState(), is(ProjectState.CLASSPATHCOMPUTED));
        assertThat(inspector.isFixerBuildCase1(), is(true));
        assertThat(jobStatus.getFailureLocations().size(), is(1));
        assertThat(jobStatus.getFailureNames().size(), is(1));

        verify(notifierEngine, times(1)).notify(anyString(), anyString());
        verify(serializerEngine, times(1)).serialize(anyListOf(SerializedData.class), eq(SerializerType.INSPECTOR4BEARS));
    }

    @Test
    public void testPassingPassingProject() throws IOException {
        int buildIdPassing = 201938881;
        int buildIdPreviousPassing = 201938325;


        Path tmpDirPath = Files.createTempDirectory("test_bears2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        Build passingBuild = BuildHelper.getBuildFromId(buildIdPassing, null);
        Build previousPassingBuild = BuildHelper.getBuildFromId(buildIdPreviousPassing, null);


        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(passingBuild, previousPassingBuild, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, "test");

        List<AbstractDataSerializer> serializers = new ArrayList<>();
        List<AbstractNotifier> notifiers = new ArrayList<>();

        List<SerializerEngine> serializerEngines = new ArrayList<>();
        SerializerEngine serializerEngine = mock(SerializerEngine.class);
        serializerEngines.add(serializerEngine);

        List<NotifierEngine> notifierEngines = new ArrayList<>();
        NotifierEngine notifierEngine = mock(NotifierEngine.class);
        notifierEngines.add(notifierEngine);

        serializers.add(new InspectorSerializer4Bears(serializerEngines));

        notifiers.add(new FixerBuildNotifier(notifierEngines));

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.BEARS);

        ProjectInspector4Bears inspector = new ProjectInspector4Bears(buildToBeInspected, tmpDir.getAbsolutePath(), serializers, notifiers);
        inspector.run();

        JobStatus jobStatus = inspector.getJobStatus();
        assertThat(jobStatus.getState(), is(ProjectState.CLASSPATHCOMPUTED));
        assertThat(inspector.isFixerBuildCase2(), is(true));
        assertThat(jobStatus.getFailureLocations().size(), is(1));
        assertThat(jobStatus.getFailureNames().size(), is(1));

        verify(notifierEngine, times(1)).notify(anyString(), anyString());
        verify(serializerEngine, times(1)).serialize(anyListOf(SerializedData.class), eq(SerializerType.INSPECTOR4BEARS));
    }
}
