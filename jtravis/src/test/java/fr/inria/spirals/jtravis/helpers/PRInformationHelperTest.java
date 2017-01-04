package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.Config;
import fr.inria.spirals.jtravis.entities.PRInformation;
import fr.inria.spirals.jtravis.entities.Repository;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 04/01/2017.
 */
public class PRInformationHelperTest {

    private static Build expectedBuild() {
        Build expectedBuild = new Build();
        expectedBuild.setId(187029370);
        expectedBuild.setCommitId(53410591);
        expectedBuild.setRepositoryId(2800492);
        expectedBuild.setNumber("2390");
        expectedBuild.setPullRequest(true);
        expectedBuild.setPullRequestTitle("Typo and wording fixes");
        expectedBuild.setPullRequestNumber(1079);
        expectedBuild.setJobIds(Arrays.asList(new Integer[]{187029371}));
        expectedBuild.setDuration(462);
        expectedBuild.setState("passed");
        expectedBuild.setStartedAt(TestUtils.getDate(2016,12,27,18,02,53));
        expectedBuild.setFinishedAt(TestUtils.getDate(2016,12,27,18,10,35));
        expectedBuild.setRepository(null);

        Config expectedConfig = new Config();
        expectedConfig.setLanguage("java");
        expectedBuild.setConfig(expectedConfig);

        Commit commit = new Commit();
        commit.setSha("982bbbc0697b063bf04306a45fe087a5aec3e1f0");
        commit.setBranch("master");
        commit.setCommittedAt(TestUtils.getDate(2016,12,27,17,59,07));
        commit.setAuthorName("Lionel Seinturier");
        commit.setCommitterEmail("noreply@github.com");
        commit.setAuthorEmail("Lionel.Seinturier@univ-lille1.fr");
        commit.setCommitterName("GitHub");
        commit.setCompareUrl("https://github.com/INRIA/spoon/pull/1079");
        commit.setMessage("Typo and wording fixes");
        expectedBuild.setCommit(commit);

        return expectedBuild;
    }

    private static PRInformation expectedPRInfo() {
        PRInformation prInfo = new PRInformation();

        Repository prRepo = new Repository();
        prRepo.setId(27972573);
        prRepo.setSlug("seintur/spoon");
        prRepo.setActive(true);
        prRepo.setDescription("Spoon is a library for analyzing and transforming Java source code.");
        prInfo.setOtherRepo(prRepo);

        Commit headCommit = new Commit();
        headCommit.setSha("567bbabd0c21214107dd8eb23edaf1e31c6ef1b3");
        headCommit.setBranch("doc-typo-wording-fixes");
        headCommit.setCommitterName("Lionel Seinturier");
        headCommit.setCommitterEmail("Lionel.Seinturier@univ-lille1.fr");
        headCommit.setMessage("Typo and wording fixes");
        headCommit.setCommittedAt(TestUtils.getDate(2016, 12, 27,18,59,07));
        headCommit.setCompareUrl("https://github.com/seintur/spoon/commit/567bbabd0c21214107dd8eb23edaf1e31c6ef1b3");
        prInfo.setHead(headCommit);

        Commit baseCommit = new Commit();
        baseCommit.setSha("2b13f0f7e82f805b2480f6ff759b2d2e8debb493");
        baseCommit.setBranch("master");
        baseCommit.setCommitterName("Дмитрий");
        baseCommit.setCommitterEmail("dm1998@list.ru");
        baseCommit.setMessage("Typo: fixes a typo in doc/gradle.md");
        baseCommit.setCommittedAt(TestUtils.getDate(2016, 12, 27,15,46,43));
        baseCommit.setCompareUrl("https://github.com/INRIA/spoon/commit/2b13f0f7e82f805b2480f6ff759b2d2e8debb493");
        prInfo.setBase(baseCommit);

        return prInfo;
    }

    @Test
    public void testGetBuildWithPR() {
        PRInformation expectedPRInfo = expectedPRInfo();
        PRInformation obtainedPRInfo = PRInformationHelper.getPRInformationFromBuild(expectedBuild());

        assertEquals(expectedPRInfo, obtainedPRInfo);
    }
}
