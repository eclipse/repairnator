package fr.inria.spirals.repairnator.realtime;

import static fr.inria.spirals.repairnator.config.RepairnatorConfig.PIPELINE_MODE;

import fr.inria.jtravis.entities.Repository;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestRTScanner {

    @Test
    public void testRepositoryWithoutSuccessfulBuildIsNotInteresting() {
        String slug = "surli/failingProject";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.REPAIR);
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }

    @Test
    public void testRepositoryWithoutCheckstyleIsNotInteresting() {
        String slug = "surli/test-repairnator";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.CHECKSTYLE);
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }

    @Test
    public void testRepositoryWithoutCheckstyleIsInteresting() {
        String slug = "repairnator/embedded-cassandra";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.CHECKSTYLE);
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertTrue(result);
    }

    @Test
    public void testRepositoryWithSuccessfulBuildIsInteresting() {
        String slug = "INRIA/spoon";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.REPAIR);
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertTrue(result);
    }

    @Test
    public void testRepositoryWithoutJavaLanguageIsNotInteresting() {
        String slug = "rails/rails";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.REPAIR);
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }

    /**
     * Note this test might fail locally if you don't have activeMQ
     * In that case this test can be temporarily be commented out
     * Also this build is taken from Tailp/travisplay, so if
     * fetch another fail build from there or from another repo
     * if 560996872 disappears in the future.
     */
    @Test
    public void testActiveMQRunnerConnection()
    {
        int buildId = 560996872;
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);
        config.setPipelineMode(PIPELINE_MODE.KUBERNETES.name());
        config.setActiveMQUrl("tcp://localhost:61616");
        config.setActiveMQQueueName("testing");

        Optional<Build> optionalBuild = config.getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        rtScanner.submitIfBuildIsInteresting(optionalBuild.get());
        assertEquals("560996872",rtScanner.receiveFromActiveMQQueue());
    }
}
