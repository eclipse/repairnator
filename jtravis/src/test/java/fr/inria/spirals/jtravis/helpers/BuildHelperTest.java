package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Config;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Repository;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 22/12/2016.
 */
public class BuildHelperTest {

    private static Build expectedBuildWithoutPR() {
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

        Config expectedConfig = new Config();
        expectedConfig.setLanguage("java");
        expectedBuild.setConfig(expectedConfig);

        Job expectedJob = new Job();
        expectedJob.setId(185719844);
        expectedJob.setCommitId(53036982);
        expectedJob.setRepositoryId(2800492);
        expectedJob.setAllowFailure(false);
        expectedJob.setBuildId(185719843);
        expectedJob.setFinishedAt(TestUtils.getDate(2016,12,21,9,56,41));
        expectedJob.setLogId(135819715);
        expectedJob.setNumber("2373.1");
        expectedJob.setQueue("builds.gce");
        expectedJob.setState("passed");
        expectedJob.setStartedAt(TestUtils.getDate(2016,12,21,9,49,46));

        expectedJob.setConfig(expectedConfig);
        expectedBuild.addJob(expectedJob);

        return expectedBuild;
    }

    private static Build expectedBuildWithPR() {
        Build expectedBuild = new Build();
        expectedBuild.setId(186814810);
        expectedBuild.setCommitId(53352968);
        expectedBuild.setRepositoryId(2800492);
        expectedBuild.setNumber("2387");
        expectedBuild.setPullRequest(true);
        expectedBuild.setPullRequestTitle("Query improvements");
        expectedBuild.setPullRequestNumber(1076);
        expectedBuild.setJobIds(Arrays.asList(new Integer[]{186814811}));
        expectedBuild.setDuration(474);
        expectedBuild.setState("passed");
        expectedBuild.setStartedAt(TestUtils.getDate(2016,12,26,17,42,4));
        expectedBuild.setFinishedAt(TestUtils.getDate(2016,12,26,17,49,58));
        expectedBuild.setRepository(null);

        Config expectedConfig = new Config();
        expectedConfig.setLanguage("java");
        expectedBuild.setConfig(expectedConfig);

        Commit commit = new Commit();
        commit.setSha("fef7aadba83da317b7de99bd67b0acffeba91591");
        commit.setBranch("master");
        commit.setCommittedAt(TestUtils.getDate(2016,12,26,17,36,34));
        commit.setAuthorName("Pavel Vojtechovsky");
        commit.setCommitterEmail("p.vojtechovsky@post.cz");
        commit.setAuthorEmail("p.vojtechovsky@post.cz");
        commit.setCommitterName("Pavel Vojtechovsky");
        commit.setCompareUrl("https://github.com/INRIA/spoon/pull/1076");
        commit.setMessage("Support of CtScanner based queries\n\n- Extraction of code to CtBaseQuery to make code cleaner and to allow\nreuse of queries\n- CtFunction can return Iterator now too");
        expectedBuild.setCommit(commit);

        Job expectedJob = new Job();
        expectedJob.setId(186814811);
        expectedJob.setRepositoryId(2800492);
        expectedJob.setBuildId(186814810);
        expectedJob.setCommitId(53352968);
        expectedJob.setLogId(136641711);
        expectedJob.setState("passed");
        expectedJob.setNumber("2387.1");
        expectedJob.setConfig(expectedConfig);
        expectedJob.setStartedAt(TestUtils.getDate(2016,12,26,17,42,4));
        expectedJob.setFinishedAt(TestUtils.getDate(2016,12,26,17,49,58));
        expectedJob.setQueue("builds.gce");
        expectedBuild.addJob(expectedJob);

        Repository prRepo = new Repository();
        prRepo.setId(72202535);
        prRepo.setSlug("pvojtechovsky/spoon");
        prRepo.setActive(true);
        prRepo.setDescription("Spoon is a library for analyzing, rewriting, transforming, transpiling Java source code. It parses source files to build a well-designed AST with powerful analysis and transformation API. It fully supports Java 8.  :beers: :sparkles:");
        expectedBuild.setPRRepository(prRepo);

        Commit headCommit = new Commit();
        headCommit.setSha("7a55cd2c526a7bbb914dacbe6ba2ddc621f23870");
        headCommit.setBranch("master");
        headCommit.setCommitterName("Martin Monperrus");
        headCommit.setMessage("doc: improve template documentation (#1068)");
        headCommit.setCommittedAt(TestUtils.getDate(2016, 12, 22,9,32,7));
        headCommit.setCompareUrl("https://github.com/INRIA/spoon/commit/7a55cd2c526a7bbb914dacbe6ba2ddc621f23870");
        expectedBuild.setHeadCommit(headCommit);

        return expectedBuild;
    }

    @Test
    public void testGetBuildFromIdWithRepoShouldReturnTheRightBuild() {
        Repository repo = new Repository();
        repo.setId(12345);

        int buildId = 185719843;

        Build expectedBuild = expectedBuildWithoutPR();
        expectedBuild.setId(buildId);
        expectedBuild.setRepository(repo);
        Build obtainedBuild = BuildHelper.getBuildFromId(buildId, repo);

        assertEquals(expectedBuild, obtainedBuild);
    }

    @Test
    public void testGetBuildFromIdWithoutRepo() {
        int buildId = 185719843;

        Build expectedBuild = expectedBuildWithoutPR();
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

    @Test
    public void testGetStatusReturnTheRightValue() {
        int buildId = 185719843;
        Build obtainedBuild = BuildHelper.getBuildFromId(buildId, null);

        assertEquals(BuildStatus.PASSED, obtainedBuild.getBuildStatus());
    }

    @Test
    public void testGetBuildWithPR() {
        int buildId = 186814810;
        Build obtainedBuild = BuildHelper.getBuildFromId(buildId, null);
        Build expectedBuild = expectedBuildWithPR();
        obtainedBuild.setRepository(null); // we cannot guarantee the information of the obtained repo...

        assertEquals(expectedBuild.getCommit(), obtainedBuild.getCommit());
        assertEquals(expectedBuild.getHeadCommit(), obtainedBuild.getHeadCommit());
        assertEquals(expectedBuild.getPRRepository(), obtainedBuild.getPRRepository());
        assertEquals(expectedBuild, obtainedBuild);
    }

    @Test
    public void testGetBuildWithPRRightMessage() {
        int buildId = 180646666;
        Build obtainedBuild = BuildHelper.getBuildFromId(buildId, null);

        assertEquals("update maven version",obtainedBuild.getHeadCommit().getMessage());
    }
}
