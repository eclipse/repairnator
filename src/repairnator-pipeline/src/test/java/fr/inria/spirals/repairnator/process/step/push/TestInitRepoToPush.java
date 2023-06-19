package fr.inria.spirals.repairnator.process.step.push;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.utils4tests.ProjectInspectorMocker;
import fr.inria.spirals.repairnator.states.PushState;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by urli on 27/04/2017.
 */
public class TestInitRepoToPush {

    private File tmpDir;

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.INFO);
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setGithubUserEmail("noreply@github.com");
        config.setGithubUserName("repairnator");
        config.setJTravisEndpoint("https://api.travis-ci.com");
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
        FileHelper.deleteFile(tmpDir);
    }

    @Test
    @Ignore("This test uses repairnator/failingProject build, which is not available anymore.")
    public void testInitRepoToPushSimpleCase() throws IOException, GitAPIException {
        long buildId = 220946365; // repairnator/failingProject build

        RepairnatorConfig repairnatorConfig = RepairnatorConfig.getInstance();
        repairnatorConfig.setClean(false);
        repairnatorConfig.setPush(true);

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_initRepoToPush").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.getProperties().getBuilds().setBuggyBuild(new fr.inria.spirals.repairnator.process.inspectors.properties.builds.Build(buildId, "", new Date()));

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true)).addNextStep(new InitRepoToPush(inspector));
        cloneStep.execute();

        assertTrue(jobStatus.getPushStates().contains(PushState.REPO_INITIALIZED));

        Git gitDir = Git.open(new File(tmpDir, "repotopush"));
        Iterable<RevCommit> logs = gitDir.log().call();

        Iterator<RevCommit> iterator = logs.iterator();
        assertTrue(iterator.hasNext());

        RevCommit commit = iterator.next();
        assertTrue(commit.getShortMessage().contains("Bug commit"));
        assertFalse(iterator.hasNext());
    }


    private Build checkBuildAndReturn(long buildId, boolean isPR) {
        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());

        Build build = optionalBuild.get();
        assertThat(build, IsNull.notNullValue());
        assertThat(buildId, Is.is(build.getId()));
        assertThat(build.isPullRequest(), Is.is(isPR));

        return build;
    }
}
