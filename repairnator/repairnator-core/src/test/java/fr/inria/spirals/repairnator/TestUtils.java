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
    public void testMatchesGithubRepoUrlWithWrongRepoUrl() {
        String repoUrl = "https://github.com/Spirals-Team/repairnator/";

        assertThat(Utils.matchesGithubRepoUrl(repoUrl), is(false));
    }

    @Test
    public void testMatchesGithubRepoUrlWithWrongRepoUrl2() {
        String repoUrl = "https://github.com/Spirals-Team/repairnator.git";

        assertThat(Utils.matchesGithubRepoUrl(repoUrl), is(false));
    }

}
