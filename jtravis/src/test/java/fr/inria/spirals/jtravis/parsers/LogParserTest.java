package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.TestUtils;
import fr.inria.spirals.jtravis.entities.BuildTool;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

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
}
