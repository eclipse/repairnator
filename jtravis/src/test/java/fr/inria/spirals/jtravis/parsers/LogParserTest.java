package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.TestUtils;
import fr.inria.spirals.jtravis.entities.*;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import javafx.scene.shape.Path;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
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
    public void testLogParserRecognizeALogAsMavenLog() throws IOException {
        String path = "./src/test/resources/UnkownBuildTool/Maven/BUILD-SUCCESS/";
        //String path = "./src/test/resources/maven-logs/";
        File folder = new File(path);
        File[] files = folder.listFiles();

        for(File file : files) {
            if(file.isFile()) {
                String fileContent = TestUtils.readFile(file.getPath());
                LogParser parser = new LogParser(fileContent);
                assertEquals(BuildTool.MAVEN, parser.getBuildTool());
            }
            else if (file.isDirectory()) {
                for(File file2 : file.listFiles()) {
                    if (file2.isFile()) {
                        String fileContent = TestUtils.readFile(file2.getPath());
                        LogParser parser = new LogParser(fileContent);
                        assertEquals(BuildTool.MAVEN, parser.getBuildTool());
                    }
                }
            }
        }


    }

}
