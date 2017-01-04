package fr.inria.spirals.jtravis.parsers;

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

        assertEquals(0, parser.getFailingTests());
        assertEquals(2, parser.getErroredTests());
        assertEquals(1, parser.getSkippingTests());
        assertEquals(9, parser.getRunningTests());
        assertEquals(6, parser.getPassingTests());

    }

    @Test
    public void testParsingSpoonLog() throws IOException {
        String path = "./src/test/resources/spoon_build_log.txt";

        String fileContent = readFile(path);
        MavenLogParser parser = new MavenLogParser(fileContent);

        assertEquals(2, parser.getFailingTests());
        assertEquals(0, parser.getErroredTests());
        assertEquals(2, parser.getSkippingTests());
        assertEquals(1114, parser.getRunningTests());
        assertEquals(1110, parser.getPassingTests());

    }
}
