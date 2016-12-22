package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildConfig;
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

        BuildConfig expectedConfig = new BuildConfig();
        expectedConfig.setLanguage("java");
        expectedConfig.setSudo("required");
        expectedConfig.setJdk(Arrays.asList(new String[]{"oraclejdk8"}));
        expectedConfig.setInstall(Arrays.asList(new String[]{ null, "mvn dependency:resolve", "pip install --user CommonMark requests", "sudo apt-get install xmlstarlet"}));

        String configScript = "# compiles and install\n" +
                "mvn install -DskipTests &&\n" +
                "\n" +
                "# checks that it works with spoon-maven-pluging\n" +
                "git clone https://github.com/square/javawriter.git &&\n" +
                "cd javawriter &&  \n" +
                "git checkout d39761f9ec25ca5bf3b7bf15d34fa2b831fed9c1 &&\n" +
                "bash ../doc/jenkins/build.sh &&\n" +
                "cd .. &&\n" +
                "rm -rf javawriter &&\n" +
                "\n" +
                "# checkstyle, license, javadoc, animal sniffer.\n" +
                "mvn verify site -DskipTests &&\n" +
                "\n" +
                "# the unit tests\n" +
                "mvn test jacoco:report  &&\n" +
                "\n" +
                "# uploading coverage, but not failing\n" +
                "mvn coveralls:report -Pcoveralls --fail-never &&\n" +
                "\n" +
                "# documentation\n" +
                "python chore/check-links-in-doc.py\n";

        expectedConfig.setScript(configScript);
        expectedConfig.setGroup("stable");
        expectedConfig.setDist("precise");
        expectedBuild.setConfig(expectedConfig);

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

    @Test
    public void testGetStatusReturnTheRightValue() {
        int buildId = 185719843;
        Build obtainedBuild = BuildHelper.getBuildFromId(buildId, null);

        assertEquals(BuildStatus.PASSED, obtainedBuild.getBuildStatus());
    }
}
