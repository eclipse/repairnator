package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.TestUtils;
import fr.inria.spirals.jtravis.entities.*;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import javafx.scene.shape.Path;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by urli on 22/02/2017.
 */
public class LogParserTest {

    @Test
    public void testLogParserRecognizeEE7LogAsMavenLog() throws IOException {
        String path = "./src/test/resources/maven-logs/javaee7log.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeSpoonLogAsMavenLog() throws IOException {
        String path = "./src/test/resources/maven-logs/spoon_build_log.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeAnotherLogAsMavenLog() throws IOException {
        String path = "./src/test/resources/maven-logs/maven-log.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeGradleErrorLogAsGradleLog() throws IOException {
        String path = "./src/test/resources/gradle-logs/multipleErrorsAndFailures.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.GRADLE, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeGradlePassingLogAsGradleLog() throws IOException {
        String path = "./src/test/resources/gradle-logs/passingLog.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.GRADLE, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeGradleRootLogAsGradleLog() throws IOException {
        String path = "./src/test/resources/gradle-logs/gradle-log.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.GRADLE, parser.getBuildTool());
    }

    @Test
    public void testToEnsureBuildToolIsMaven() {
        int buildId = 218154223;

        Build build = BuildHelper.getBuildFromId(buildId, null);

        boolean result = false;
        for (Job job : build.getJobs()) {
            Log jobLog = job.getLog();

            if (jobLog != null && jobLog.getBuildTool() == BuildTool.MAVEN) {
                result = true;
            }
        }

        assertTrue(build != null);
        assertEquals(true, result);
    }

    @Test
    public void testToEnsureBuildToolIsGradle() {
        int buildId = 218180742;

        Build build = BuildHelper.getBuildFromId(buildId, null);

        boolean result = false;
        for (Job job : build.getJobs()) {
            Log jobLog = job.getLog();

            if (jobLog != null && jobLog.getBuildTool() == BuildTool.GRADLE) {
                result = true;
            }
        }

        assertTrue(build != null);
        assertEquals(true, result);
    }

    @Test
    public void testLogParserRecognizeLogAsGradleLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Gradle/";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.GRADLE, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeSuccessLog1AsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Maven/BUILD-SUCCESS/chrishantha_ jfr-flame-graph.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeSuccessLog2AsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Maven/BUILD-SUCCESS/influxdata_influxdb-java.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeSuccessLog3AsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Maven/BUILD-SUCCESS/maven-log.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeFailLog1AsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Maven/BUILD-FAIL/apache_syncope.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeFailLog2AsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Maven/BUILD-FAIL/javaee7log.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeFailLog3AsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Maven/BUILD-FAIL/spoon_build_log.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
    }


    @Test
    public void testLogParserRecognizeFailLog4AsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Maven/BUILD-FAIL/joel-costigliola_assertj-core.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeGradleNotAsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Gradle/BUILD-SUCCESSFUL/gradle-log.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertNotEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeGradle2NotAsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Gradle/BUILD-SUCCESSFUL/passingLog.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertNotEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeGradle3NotAsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Gradle/BUILD-SUCCESSFUL/bndtools_bndtools.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertNotEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeGradle4NotAsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Gradle/BUILD-FAIL/bndtools_bndtools.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertNotEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeGradle5NotAsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Gradle/BUILD-FAIL/eXist-db_exist.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertNotEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void testLogParserRecognizeGradle6NotAsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Gradle/BUILD-FAIL/multipleErrorsAndFailures.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);

        assertNotEquals(BuildTool.MAVEN, parser.getBuildTool());
    }

    @Test
    public void multipleTestRun() throws IOException {
        //Test is NOK
        //testLogParserRecognizeSuccessLog2AsMavenLog();

        //Test is OK
        testLogParserRecognizeSuccessLog1AsMavenLog();
        testLogParserRecognizeSuccessLog3AsMavenLog();
        testLogParserRecognizeFailLog1AsMavenLog();
        testLogParserRecognizeFailLog2AsMavenLog();
        testLogParserRecognizeFailLog3AsMavenLog();
        testLogParserRecognizeFailLog4AsMavenLog();

        testLogParserRecognizeGradleNotAsMavenLog();
        testLogParserRecognizeGradle2NotAsMavenLog();
        testLogParserRecognizeGradle3NotAsMavenLog();
        testLogParserRecognizeGradle4NotAsMavenLog();
        testLogParserRecognizeGradle5NotAsMavenLog();
        testLogParserRecognizeGradle6NotAsMavenLog();
    }

}
