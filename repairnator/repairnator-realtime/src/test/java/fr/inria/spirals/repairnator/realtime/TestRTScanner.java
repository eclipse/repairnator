package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Repository;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestRTScanner {

    @Test
    public void testRepositoryWithoutSuccessfulBuildIsNotInteresting() {
        String slug = "surli/failingProject";
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }

    @Test
    public void testRepositoryWithSuccessfulBuildIsInteresting() {
        String slug = "INRIA/spoon";
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertTrue(result);
    }

    @Test
    public void testRepositoryWithoutJavaLanguageIsNotInteresting() {
        String slug = "rails/rails";
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }
}
