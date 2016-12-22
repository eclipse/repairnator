package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.JobConfig;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 22/12/2016.
 */
public class JobHelperTest {

    @Test
    public void testGetJobFromId() {
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

        JobConfig expectedConfig = new JobConfig();
        expectedConfig.setLanguage("java");
        expectedConfig.setSudo("required");
        expectedConfig.setJdk("oraclejdk8");
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
        expectedConfig.setOs("linux");

        expectedJob.setConfig(expectedConfig);

        Job obtainedJob = JobHelper.getJobFromId(185719844);
        assertEquals(expectedJob, obtainedJob);
    }
}
