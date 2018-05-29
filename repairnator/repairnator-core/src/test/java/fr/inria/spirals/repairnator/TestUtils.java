package fr.inria.spirals.repairnator;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestUtils {

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
    public void testMatchesGithubRepoUrlWithWrongRepoUrl() {
        String repoUrl = "https://github.com/Spirals-Team/repairnator/";

        assertThat(Utils.matchesGithubRepoUrl(repoUrl), is(false));
    }

    @Test
    public void testMatchesGithubRepoUrlWithWrongRepoUrl2() {
        String repoUrl = "https://github.com/Spirals-Team/repairnator.git";

        assertThat(Utils.matchesGithubRepoUrl(repoUrl), is(false));
    }

    @Test
    public void testMatchesGithubRepoUrlWithWrongRepoUrlWithNumber() {
        String repoUrl = "https://github.com/Sp1r4ls-T34m/r3p41rn4t0r.git";

        assertThat(Utils.matchesGithubRepoUrl(repoUrl), is(false));
    }

}
