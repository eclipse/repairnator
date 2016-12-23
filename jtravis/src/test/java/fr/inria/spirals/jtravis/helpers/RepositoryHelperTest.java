package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Repository;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by urli on 21/12/2016.
 */
public class RepositoryHelperTest {

    @Test
    public void testGetSpoonRepoFromSlugWorks() {
        Repository spoonRepo = RepositoryHelper.getRepositoryFromSlug("INRIA/spoon");

        assertEquals("INRIA/spoon",spoonRepo.getSlug());
        assertEquals(2800492, spoonRepo.getId());
        assertTrue(spoonRepo.getLastBuildId() > 0);
    }

    @Test
    public void testGetSpoonRepoFromIdWorks() {
        Repository spoonRepo = RepositoryHelper.getRepositoryFromId(2800492);

        assertEquals("INRIA/spoon",spoonRepo.getSlug());
        assertEquals(2800492, spoonRepo.getId());
        assertTrue(spoonRepo.getLastBuildId() > 0);
    }

    @Test
    public void testGetUnknownRepoThrowsException() {
        Repository unknownRepo = RepositoryHelper.getRepositoryFromSlug("surli/unknown");
        assertTrue(unknownRepo == null);
    }
}
