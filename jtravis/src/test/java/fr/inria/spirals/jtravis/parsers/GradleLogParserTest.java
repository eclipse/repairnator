package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.TestUtils;
import fr.inria.spirals.jtravis.entities.TestsInformation;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 22/02/2017.
 */
public class GradleLogParserTest {

    @Test
    public void testParsingPassingLogReturnsRightInfoTest() throws IOException {
        String path = "./src/test/resources/gradle-logs/passingLog.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);
        TestsInformation infoTest = parser.getTestsInformation();

        assertEquals(0, infoTest.getFailing());
        assertEquals(0, infoTest.getErrored());
        assertEquals(0, infoTest.getSkipping());
        assertEquals(0, infoTest.getRunning());
        assertEquals(0, infoTest.getPassing());
    }

    @Test
    public void testParsingMultipleErrorsLogReturnsRightInfoTest() throws IOException {
        String path = "./src/test/resources/gradle-logs/multipleErrorsAndFailures.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);
        TestsInformation infoTest = parser.getTestsInformation();

        assertEquals(4, infoTest.getFailing());
        assertEquals(0, infoTest.getErrored());
        assertEquals(0, infoTest.getSkipping());
        assertEquals(171, infoTest.getRunning());
        assertEquals(167, infoTest.getPassing());
    }
}
