package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.Repository;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 22/12/2016.
 */
public class BuildHelperTest {

    private static Build expectedBuild() {
        Build expectedBuild = new Build();
        expectedBuild.setNumber("2373");
        expectedBuild.setState("passed");
        expectedBuild.setStartedAt(TestUtils.getDate(2016, 12, 21, 9, 49, 46));
        expectedBuild.setFinishedAt(TestUtils.getDate(2016, 12, 21, 9, 56, 41));
        expectedBuild.setDuration(415);
        expectedBuild.setCommitId(53036982);
        expectedBuild.setRepositoryId(2800492);
        expectedBuild.setJobIds(Arrays.asList(new Integer[]{185719844}));

        Commit commit = new Commit();
        commit.setSha("d283ce5727f47c854470e64ac25144de5d8e6c05");
        commit.setMessage("test: add test for method parameter templating (#1064)");
        commit.setCompareUrl("https://github.com/INRIA/spoon/compare/3c5ab0fe7a89...d283ce5727f4");
        commit.setBranch("master");
        commit.setAuthorName("Martin Monperrus");
        commit.setAuthorEmail("monperrus@users.noreply.github.com");
        commit.setCommitterEmail("simon.urli@gmail.com");
        commit.setCommitterName("Simon Urli");
        commit.setCommittedAt(TestUtils.getDate(2016,12,21,9,48,50));
        expectedBuild.setCommit(commit);

        return expectedBuild;
    }

    @Test
    public void testGetBuildFromIdWithRepoShouldReturnTheRightBuild() {
        Repository repo = new Repository();
        repo.setId(12345);

        int buildId = 185719843;

        Build expectedBuild = expectedBuild();
        expectedBuild.setId(buildId);
        expectedBuild.setRepository(repo);
        Build obtainedBuild = BuildHelper.getBuildFromId(buildId, repo);

        assertEquals(expectedBuild, obtainedBuild);
    }

    @Test
    public void testGetBuildFromIdWithoutRepo() {
        int buildId = 185719843;

        Build expectedBuild = expectedBuild();
        expectedBuild.setId(buildId);
        Build obtainedBuild = BuildHelper.getBuildFromId(buildId, null);

        assertEquals(expectedBuild, obtainedBuild);
    }

    @Test
    public void testGetRepoAfterCreatingBuildWithoutRepo() {
        int buildId = 185719843;
        Build obtainedBuild = BuildHelper.getBuildFromId(buildId, null);

        Repository obtainedRepo = obtainedBuild.getRepository();

        assertEquals("INRIA/spoon", obtainedRepo.getSlug());
        assertEquals(2800492, obtainedRepo.getId());
    }
}
