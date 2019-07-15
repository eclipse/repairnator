package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Repository;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
