package fr.inria.spirals.repairnator.process.step;

import ch.qos.logback.classic.Level;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.utils4tests.ProjectInspectorMocker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 21/02/2017.
 */
public class TestAbstractStep {

    public class AbstractStepNop extends AbstractStep {

        public AbstractStepNop(ProjectInspector inspector) {
            super(inspector, false);
        }

        @Override
        protected StepStatus businessExecute() {
            return StepStatus.buildSuccess(this);
        }
    }

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testGetPomOnSimpleProject() {
        String localRepoPath = "./src/test/resources/test-abstractstep/simple-maven-project";
        JobStatus jobStatus = new JobStatus(localRepoPath);
        ProjectInspector mockInspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, localRepoPath);

        AbstractStep step1 = new AbstractStepNop(mockInspector);

        String expectedPomPath = localRepoPath+"/pom.xml";

        assertThat(step1.getPom(), is(expectedPomPath));
    }

    @Test
    public void testGetPomWhenNotFoundShouldSetStopFlag() {
        String localRepoPath = "./unkown-path";
        JobStatus jobStatus = new JobStatus(localRepoPath);
        ProjectInspector mockInspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, localRepoPath);

        AbstractStep step1 = new AbstractStepNop(mockInspector);

        String expectedPomPath = localRepoPath+"/pom.xml";

        // return this path but set the flag to stop
        assertThat(step1.getPom(), is(expectedPomPath));
        assertThat(step1.isShouldStop(), is(true));
    }

    @Test
    public void testGetPomWithComplexMavenProjectShouldSetRepoPath() {
        String localRepoPath = "./src/test/resources/test-abstractstep/complex-maven-project";
        JobStatus jobStatus = new JobStatus(localRepoPath);
        ProjectInspector mockInspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, localRepoPath);

        AbstractStep step1 = new AbstractStepNop(mockInspector);

        String expectedPomPath = localRepoPath+"/a-submodule";

        String obtainedPom = step1.getPom();
        assertThat(jobStatus.getPomDirPath(), is(expectedPomPath));
    }

    @Test
    public void testGetPomOnProjectWithSubModule() throws IOException, InterruptedException {
        String projectUrl = "https://github.com/Spirals-Team/repairnator.git";

        File tmpDir = Files.createTempDirectory("test_get_pom_project_with_submodule").toFile();
        tmpDir.deleteOnExit();

        String[] gitClone = new String[]{"git", "clone", "--recurse-submodules", projectUrl};
        ProcessBuilder processBuilder = new ProcessBuilder().command(gitClone).directory(tmpDir);

        Process process = processBuilder.start();
        process.waitFor();

        ProjectInspector mockInspector = mock(ProjectInspector.class);

        String localRepoPath = tmpDir.getAbsolutePath() + "/repairnator";
        JobStatus jobStatus = new JobStatus(localRepoPath);
        when(mockInspector.getRepoLocalPath()).thenReturn(localRepoPath);
        when(mockInspector.getJobStatus()).thenReturn(jobStatus);

        AbstractStep step = new AbstractStepNop(mockInspector);

        String expectedPomPath = localRepoPath+"/repairnator/pom.xml";
        String actualPomPath = step.getPom();

        assertThat(actualPomPath, is(expectedPomPath));
    }

}
