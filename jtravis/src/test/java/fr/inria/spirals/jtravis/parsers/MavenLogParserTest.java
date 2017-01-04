package fr.inria.spirals.jtravis.parsers;

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

    private static String readFile(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));

        String result = "";

        while (reader.ready()) {
            result += reader.readLine()+"\n";
        }
        return result;
    }

    @Test
    public void testParsingJavaEE7Log() throws IOException {
        String path = "./src/test/resources/javaee7log.txt";

        String fileContent = readFile(path);
        MavenLogParser parser = new MavenLogParser(fileContent);
        TestsInformation infoTest = parser.getTestsInformation();

        assertEquals(0, infoTest.getFailing());
        assertEquals(2, infoTest.getErrored());
        assertEquals(1, infoTest.getSkipping());
        assertEquals(9, infoTest.getRunning());
        assertEquals(6, infoTest.getPassing());

    }

    @Test
    public void testParsingSpoonLog() throws IOException {
        String path = "./src/test/resources/spoon_build_log.txt";

        String fileContent = readFile(path);
        MavenLogParser parser = new MavenLogParser(fileContent);
        TestsInformation infoTest = parser.getTestsInformation();

        assertEquals(2, infoTest.getFailing());
        assertEquals(0, infoTest.getErrored());
        assertEquals(2, infoTest.getSkipping());
        assertEquals(1114, infoTest.getRunning());
        assertEquals(1110, infoTest.getPassing());
    }
}
