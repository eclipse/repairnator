package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.TestUtils;
import fr.inria.spirals.jtravis.entities.TestsInformation;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

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

    @Test
    public void testParsingLog3ReturnsRightInfoTest() throws IOException {
        String path = "./src/test/resources/maven-logs/log3.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);
        TestsInformation infoTest = parser.getTestsInformation();

        assertEquals(1, infoTest.getFailing());
        assertEquals(0, infoTest.getErrored());
        assertEquals(12, infoTest.getSkipping());
        assertEquals(1392, infoTest.getRunning());
        assertEquals(1379, infoTest.getPassing());
    }

    @Test
    public void testParsingLogLibrepairReturnsRightInfoTest() throws IOException {
        String path = "./src/test/resources/maven-logs/librepair.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);
        TestsInformation infoTest = parser.getTestsInformation();

        assertEquals(0, infoTest.getFailing());
        assertEquals(0, infoTest.getErrored());
        assertEquals(2, infoTest.getSkipping());
        assertEquals(96, infoTest.getRunning());
        assertEquals(94, infoTest.getPassing());
    }

    @Test
    public void testDetailedParsingLogLibrepairReturnsRightInfoTest() throws IOException {
        String path = "./src/test/resources/maven-logs/librepair.txt";

        String fileContent = TestUtils.readFile(path);
        LogParser parser = new LogParser(fileContent);
        List<TestsInformation> infoTest = parser.getDetailedTestsInformation();


        assertEquals(52, infoTest.get(0).getRunning());
        assertEquals(0, infoTest.get(0).getFailing());
        assertEquals(0, infoTest.get(0).getSkipping());
        assertEquals(0, infoTest.get(0).getErrored());
        assertEquals(52, infoTest.get(0).getPassing());

        assertEquals(5, infoTest.get(1).getRunning());
        assertEquals(0,  infoTest.get(1).getFailing());
        assertEquals(2,  infoTest.get(1).getSkipping());
        assertEquals(0,  infoTest.get(1).getErrored());
        assertEquals(3, infoTest.get(1).getPassing());

        assertEquals(39, infoTest.get(2).getRunning());
        assertEquals(0,  infoTest.get(2).getFailing());
        assertEquals(0,  infoTest.get(2).getSkipping());
        assertEquals(0,  infoTest.get(2).getErrored());
        assertEquals(39, infoTest.get(2).getPassing());

    }
}
