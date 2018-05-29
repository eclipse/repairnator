package fr.inria.spirals.repairnator;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestUtils {

    @Test
    public void testGithubUserNamePattern() {
        String userName = "lucesape";
        assertThat(userName.matches(Utils.GITHUB_USER_NAME_PATTERN), is(true));

        userName = "LucEsape";
        assertThat(userName.matches(Utils.GITHUB_USER_NAME_PATTERN), is(true));

        userName = "luc3s4p3";
        assertThat(userName.matches(Utils.GITHUB_USER_NAME_PATTERN), is(true));

        userName = "luc-esape";
        assertThat(userName.matches(Utils.GITHUB_USER_NAME_PATTERN), is(true));

        userName = "luc-esape-copy";
        assertThat(userName.matches(Utils.GITHUB_USER_NAME_PATTERN), is(true));

        userName = "-luc-esape-";
        assertThat(userName.matches(Utils.GITHUB_USER_NAME_PATTERN), is(false));

        userName = "luc_esape";
        assertThat(userName.matches(Utils.GITHUB_USER_NAME_PATTERN), is(false));
    }

    @Test
    public void testGithubRepoNamePattern() {
        String repoName = "repairnator";
        assertThat(repoName.matches(Utils.GITHUB_REPO_NAME_PATTERN), is(true));

        repoName = "RepairNator";
        assertThat(repoName.matches(Utils.GITHUB_REPO_NAME_PATTERN), is(true));

        repoName = "r3p41rn4t0r";
        assertThat(repoName.matches(Utils.GITHUB_REPO_NAME_PATTERN), is(true));

        repoName = "repair-nator";
        assertThat(repoName.matches(Utils.GITHUB_REPO_NAME_PATTERN), is(true));

        repoName = "repair_nator";
        assertThat(repoName.matches(Utils.GITHUB_REPO_NAME_PATTERN), is(true));

        repoName = "repair.nator";
        assertThat(repoName.matches(Utils.GITHUB_REPO_NAME_PATTERN), is(true));

        repoName = "repair$nator";
        assertThat(repoName.matches(Utils.GITHUB_REPO_NAME_PATTERN), is(false));

        repoName = "repairnator.git";
        assertThat(repoName.matches(Utils.GITHUB_REPO_NAME_PATTERN), is(false));
    }

    @Test
    public void testMatchesGithubRepoUrlWithRightRepoUrl() {
        String repoUrl = "https://github.com/Spirals-Team/repairnator";

        assertThat(Utils.matchesGithubRepoUrl(repoUrl), is(true));
    }

    @Test
    public void testMatchesGithubRepoUrlWithRightRepoUrlWithNumber() {
        String repoUrl = "https://github.com/Sp1r4ls-T34m/r3p41rn4t0r";

        assertThat(Utils.matchesGithubRepoUrl(repoUrl), is(true));
    }

    @Test
    public void testMatchesGithubRepoUrlWithWrongRepoUrlWithSlashAtTheEnd() {
        String repoUrl = "https://github.com/Spirals-Team/repairnator/";

        assertThat(Utils.matchesGithubRepoUrl(repoUrl), is(false));
    }

    @Test
    public void testMatchesGithubRepoUrlWithWrongRepoUrlWithDotGitAtTheEnd() {
        String repoUrl = "https://github.com/Spirals-Team/repairnator.git";

        assertThat(Utils.matchesGithubRepoUrl(repoUrl), is(false));
    }

}
