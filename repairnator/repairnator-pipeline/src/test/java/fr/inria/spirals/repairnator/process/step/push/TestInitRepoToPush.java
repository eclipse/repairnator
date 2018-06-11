package fr.inria.spirals.repairnator.process.step.push;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.states.PushState;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.config.RepairnatorConfigException;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 27/04/2017.
 */
public class TestInitRepoToPush {

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.INFO);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testInitRepoToPushSimpleCase() throws IOException, GitAPIException, RepairnatorConfigException {
        long buildId = 207924136; // surli/failingProject build

        RepairnatorConfig repairnatorConfig = RepairnatorConfig.getInstance();
        repairnatorConfig.setClean(false);
        repairnatorConfig.setPush(true);

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_initRepoToPush");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getRepoToPushLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repotopush");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true)).setNextStep(new InitRepoToPush(inspector));
        cloneStep.execute();

        assertThat(jobStatus.getPushStates().contains(PushState.REPO_INITIALIZED), is(true));

        Git gitDir = Git.open(new File(tmpDir, "repotopush"));
        Iterable<RevCommit> logs = gitDir.log().call();

        Iterator<RevCommit> iterator = logs.iterator();
        assertThat(iterator.hasNext(), is(true));

        RevCommit commit = iterator.next();
        assertThat(commit.getShortMessage(), containsString("Bug commit"));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testInitRepoShouldRemoveNotificationInTravisYML() throws IOException {
        long buildId = 331637757;

        RepairnatorConfig repairnatorConfig = RepairnatorConfig.getInstance();
        repairnatorConfig.setClean(false);
        repairnatorConfig.setPush(true);

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_initRepoToPush");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getRepoToPushLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repotopush");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true)).setNextStep(new InitRepoToPush(inspector));
        cloneStep.execute();

        assertThat(jobStatus.getPushStates().contains(PushState.REPO_INITIALIZED), is(true));
        File bak = new File(tmpDir.getAbsolutePath()+"/repotopush/bak.travis.yml");
        File travis = new File(tmpDir.getAbsolutePath()+"/repotopush/.travis.yml");

        assertTrue(bak.exists());
        assertTrue(travis.exists());

        boolean detected = false;
        List<String> lines = Files.readAllLines(travis.toPath());
        for (String l : lines) {
            if (l.contains("notification")) {
                assertTrue(l.trim().startsWith("#"));
                detected = true;
            }
            if (l.contains("script")) {
                assertFalse(l.trim().startsWith("#"));
            }
        }

        assertTrue(detected);
    }
}
