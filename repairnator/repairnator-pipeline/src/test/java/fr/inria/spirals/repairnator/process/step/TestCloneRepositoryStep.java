package fr.inria.spirals.repairnator.process.step;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 21/02/2017.
 */
public class TestCloneRepositoryStep {

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testCloneMasterBuild() throws IOException {
        int buildId = 207924136; // surli/failingProject build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_clone");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        cloneStep.execute();

        assertThat(jobStatus.getPipelineState(), is(PipelineState.CLONABLE));
        assertThat(cloneStep.getPipelineState(), is(PipelineState.CLONABLE));
        assertThat(cloneStep.shouldStop, is(false));

        Git gitDir = Git.open(new File(tmpDir, "repo"));
        Ref ref = gitDir.getRepository().exactRef("HEAD");

        assertThat(ref.isSymbolic(), is(true));

        ref = ref.getTarget();

        assertThat(ref.getObjectId().getName(), not(build.getCommit().getSha())); // no check out yet
    }

    @Test
    public void testCloneBuildWithSubmodule() throws IOException {
        int buildId = 355839305; // surli/failingProject build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_clone");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        cloneStep.execute();

        assertThat(jobStatus.getPipelineState(), is(PipelineState.CLONABLE));
        assertThat(cloneStep.getPipelineState(), is(PipelineState.CLONABLE));
        assertThat(cloneStep.shouldStop, is(false));

        File licenceInSubmodule = new File(tmpDirPath.toFile(), "repo/grakn-spec/LICENSE");
        assertThat("Submodule are not supported", licenceInSubmodule.exists(), is(true));
    }
}
