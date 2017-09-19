package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

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
        commit.setId(53036982);
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
        expectedJob.setNumber("2373.1");
        expectedJob.setQueue("builds.gce");
        expectedJob.setState("passed");
        expectedJob.setStartedAt(TestUtils.getDate(2016,12,21,9,49,46));

        expectedJob.setConfig(expectedConfig);
        expectedBuild.addJob(expectedJob);

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
    public void testGetBuildToolFromBuildRecognizeTool() {
        int buildId = 185719843;
        Build obtainedBuild = BuildHelper.getBuildFromId(buildId, null);

        assertEquals(BuildTool.MAVEN, obtainedBuild.getBuildTool());
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
    public void testGetLastFailingBuildBeforeGivenBuild() {
        int buildId = 197104485;
        Build passingBuild = BuildHelper.getBuildFromId(buildId, null);

        int expectedBuildId = 197067445;
        Build obtainedBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(passingBuild, BuildStatus.FAILED);

        assertTrue(obtainedBuild != null);
        assertEquals(expectedBuildId, obtainedBuild.getId());
    }

    @Test
    public void testGetNextPassingBuildAfterGivenFailingBuild() {
        int buildId = 197067445;
        Build failingBuild = BuildHelper.getBuildFromId(buildId, null);

        int expectedBuildId = 197104485;
        Build obtainedBuild = BuildHelper.getNextBuildOfSameBranchOfStatusAfterBuild(failingBuild, BuildStatus.PASSED);

        assertTrue(obtainedBuild != null);
        assertEquals(expectedBuildId, obtainedBuild.getId());
    }

    @Test
    public void testGetLastErroredBuildBeforeGivenBuild() {
        int buildId = 197233494;
        Build passingBuild = BuildHelper.getBuildFromId(buildId, null);

        int expectedBuildId = 193970329;
        Build obtainedBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(passingBuild, BuildStatus.ERRORED);

        assertTrue(obtainedBuild != null);
        assertEquals(expectedBuildId, obtainedBuild.getId());
    }

    @Test
    public void testGetNextPassingBuildAfterGivenErroredBuild() {
        int  buildId = 193970329;
        Build erroredBuild = BuildHelper.getBuildFromId(buildId, null);

        int expectedBuildId = 193992095;
        Build obtainedBuild = BuildHelper.getNextBuildOfSameBranchOfStatusAfterBuild(erroredBuild, BuildStatus.PASSED);

        assertTrue(obtainedBuild != null);
        assertEquals(expectedBuildId, obtainedBuild.getId());
    }

    @Test
    public void testGetLastBuildJustBeforeGivenBuild() {
        int buildId = 191511078;
        Build passingBuild = BuildHelper.getBuildFromId(buildId, null);

        int expectedBuildId = 191412122;
        Build obtainedBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(passingBuild, null);

        assertTrue(obtainedBuild != null);
        assertEquals(expectedBuildId, obtainedBuild.getId());
    }

    @Test
    public void testGetNextBuildJustAfterGivenBuild() {
        int buildId = 191412122;
        Build passingBuild = BuildHelper.getBuildFromId(buildId, null);


        int expectedBuildId = 191511078;
        Build obtainedBuild = BuildHelper.getNextBuildOfSameBranchOfStatusAfterBuild(passingBuild, null);

        assertTrue(obtainedBuild != null);
        assertEquals(expectedBuildId, obtainedBuild.getId());
    }

    @Test
    public void testGetLastBuildWorksOnMaster() {
        int buildId = 207455891;
        Build currentBuild = BuildHelper.getBuildFromId(buildId, null);

        int expectedBuildId = 207113449;
        Build obtainedBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(currentBuild, null);

        assertTrue(obtainedBuild != null);
        assertEquals(expectedBuildId, obtainedBuild.getId());
    }

    @Test
    public void testGetNextBuildWorksOnMaster() {
        int buildId = 207113449;
        Build currentBuild = BuildHelper.getBuildFromId(buildId, null);

        int expectedBuildId = 207455891;
        Build obtainedBuild = BuildHelper.getNextBuildOfSameBranchOfStatusAfterBuild(currentBuild, null);

        assertTrue(obtainedBuild != null);
        assertEquals(expectedBuildId, obtainedBuild.getId());
    }

    @Test
    public void testGetLastBuildJustBeforeGivenBuildOnTheSameBranch() {
        int buildId = 208181440;
        Build currentBuild = BuildHelper.getBuildFromId(buildId, null);

        int expectedBuildId = 208116073;
        Build obtainedBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(currentBuild, null);

        assertTrue(obtainedBuild != null);
        assertEquals(expectedBuildId, obtainedBuild.getId());
    }

    @Test
    public void testGetNextBuildJustAfterGivenBuildOnTheSameBranch() {
        int buildId = 208116073;

        Build currentBuild = BuildHelper.getBuildFromId(buildId, null);

        int expectedBuildId = 208181440;
        Build obtainedBuild = BuildHelper.getNextBuildOfSameBranchOfStatusAfterBuild(currentBuild, null);

        assertTrue(obtainedBuild != null);
        assertEquals(expectedBuildId, obtainedBuild.getId());
    }

    @Test
    public void testGetTheLastBuildNumberOfADate() {
        Date date = TestUtils.getDate(2017, 03, 16, 22, 59, 59);

        String slug = "Spirals-Team/librepair";

        int expectedBuildNumber = 215;

        int obtainedBuildNumber = BuildHelper.getTheLastBuildNumberOfADate(slug, date, 0,false);

        assertEquals(expectedBuildNumber, obtainedBuildNumber);
    }

    @Test
    public void testGetTheLastBuildNumberOfADate2() {
        Date date = TestUtils.getDate(2017, 03, 14, 22, 59, 59);

        String slug = "Spirals-Team/librepair";

        int expectedBuildNumber = 189;

        int obtainedBuildNumber = BuildHelper.getTheLastBuildNumberOfADate(slug, date, 0,false);

        assertEquals(expectedBuildNumber, obtainedBuildNumber);
    }

    @Test
    public void testGetBuildsFromRepositoryInTimeInterval() {
        Repository repo = new Repository();
        repo.setSlug("Spirals-Team/librepair");

        Date initialDate = TestUtils.getDate(2017, 03, 13, 23, 00, 00);

        Date finalDate = TestUtils.getDate(2017, 03, 16, 22, 59, 59);
        List<Build> builds = BuildHelper.getBuildsFromRepositoryInTimeInterval(repo, initialDate, finalDate);

        assertTrue(builds != null);

        if (builds.size() > 1) {
            Collections.sort(builds);
        }

        assertEquals(60, builds.size());
        assertEquals(156, Integer.parseInt(builds.get(0).getNumber()));
        assertEquals(215, Integer.parseInt(builds.get(builds.size()-1).getNumber()));
    }

    @Test
    public void testGetBuildsFromRepositoryInTimeIntervalAlsoTakePR() {
        Repository repo = new Repository();
        repo.setSlug("INRIA/spoon");

        Date initialDate = TestUtils.getDate(2017, 6, 26, 20, 0, 0);

        Date finalDate = TestUtils.getDate(2017, 6, 26, 21, 0, 0);
        List<Build> builds = BuildHelper.getBuildsFromRepositoryInTimeInterval(repo, initialDate, finalDate);

        assertTrue(builds != null);

        if (builds.size() > 1) {
            Collections.sort(builds);
        }

        assertEquals(3, builds.size());
        assertEquals(3424, Integer.parseInt(builds.get(0).getNumber()));
        for (Build build : builds) {
            assertTrue(build.isPullRequest());
        }


        assertEquals(1428, builds.get(0).getPullRequestNumber());
    }

    @Test
    public void testGetLastBuildFromMasterIgnoreNumberNullBuilds() {
        String slug = "Graylog2/graylog2-server";
        Repository repo = RepositoryHelper.getRepositoryFromSlug(slug);

        Build b = repo.getLastBuild(true);
        assertTrue(b.getNumber() != null);
    }

    @Test
    public void testGetFutureBuildOfBranch() {
        int buildId = 220970612;

        Build b = BuildHelper.getBuildFromId(buildId, null);
        assertEquals("21405", b.getNumber());

        Build obtained = BuildHelper.getNextBuildOfSameBranchOfStatusAfterBuild(b, null);

        assertNotNull(obtained);
        assertEquals("21423", obtained.getNumber());
    }

    @Test
    public void testGetFutureBuildOfBranch2() {
        int buildId = 219250521;

        Build b = BuildHelper.getBuildFromId(buildId, null);
        assertEquals("21005", b.getNumber());

        Build obtained = BuildHelper.getNextBuildOfSameBranchOfStatusAfterBuild(b, null);

        assertNotNull(obtained);
        assertEquals("21420", obtained.getNumber());
    }

    @Test
    public void testGetBuildsFromSlugWithLimitDate() {
        String slug = "surli/failingProject";
        Date limitDate = TestUtils.getDate(2017, 9, 14, 0, 0, 1);

        List<Build> builds = BuildHelper.getBuildsFromSlugWithLimitDate(slug, limitDate);
        List<String> obtainedIds = new ArrayList<String>();


        for (Build b:builds)
        {
            obtainedIds.add(b.getNumber());
        }
            

        List<String> expectedIds = Arrays.asList("373","374","375");
        List<String> unexpectedIds = Arrays.asList("273","274","275");

        assertNotNull(builds);

        //Vérifie que tout les ids de la liste expectedIds sont présents dans la liste des ids retournés
        assertTrue(obtainedIds.containsAll(expectedIds));

        //Vérifie qu'aucun des ids de la liste unexpectedIds n'est présent dans la liste des ids retournés
        for (String id:unexpectedIds)
        {
            assertFalse(obtainedIds.contains(id));
        }

    }

    @Test
    public void testGetBuildsFromSlug() {

    }

    @Test
    public void testGetBuildsFromRepositoryWithLimitDate() {

    }

    @Test
    public void testGetBuildsFromRepository() {

    }

}
