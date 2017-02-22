package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.TestUtils;
import fr.inria.spirals.jtravis.entities.TestsInformation;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 03/01/2017.
 */
public class MavenLogParserTest {

    @Test
    public void testParsingJavaEE7LogReturnsRightInfoTest() throws IOException {
        String path = "./src/test/resources/maven-logs/javaee7log.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);
        TestsInformation infoTest = parser.getTestsInformation();

        assertEquals(0, infoTest.getFailing());
        assertEquals(2, infoTest.getErrored());
        assertEquals(1, infoTest.getSkipping());
        assertEquals(9, infoTest.getRunning());
        assertEquals(6, infoTest.getPassing());

    }

    @Test
    public void testParsingSpoonLogReturnsRightInfoTest() throws IOException {
        String path = "./src/test/resources/maven-logs/spoon_build_log.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);
        TestsInformation infoTest = parser.getTestsInformation();

        assertEquals(2, infoTest.getFailing());
        assertEquals(0, infoTest.getErrored());
        assertEquals(2, infoTest.getSkipping());
        assertEquals(1114, infoTest.getRunning());
        assertEquals(1110, infoTest.getPassing());
    }
}
