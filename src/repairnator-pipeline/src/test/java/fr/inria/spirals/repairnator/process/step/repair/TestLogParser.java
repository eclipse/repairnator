package fr.inria.spirals.repairnator.process.step.repair;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestLogParser {

    @Test
    public void TestLogParserTestsFailing() throws IOException {

        LogParser p = new LogParser();
        String log = loadLogFromResources("/test-logparser/test-error.dat");

        p.parse(log);

        List<LogParser.Element> tests = p.getTests();

        assertEquals(tests.size(), 1);

        LogParser.Element test = tests.get(0);

        assertEquals(test.<String>get("name"), "symbolic_examples.symbolic_example_3.NopolExampleTest");
        assertEquals(test.<Integer>get("nbTest").intValue(), 9);
        assertEquals(test.<Integer>get("nbFailure").intValue(), 9);

    }

    @Test
    public void TestLogParserCompilationError() throws IOException {

        LogParser p = new LogParser();
        String log = loadLogFromResources("/test-logparser/compilation-error.dat");

        p.parse(log);

        List<LogParser.Element> errors = p.getErrors();

        assertEquals(errors.size(), 1);

        LogParser.Element error = errors.get(0);

        assertEquals(error.<String>get("file"), "/home/javier/failingProject/src/main/java/symbolic_examples/symbolic_example_3/NopolExample.java");
        assertEquals(error.<Integer>get("line").intValue(), 22);
        assertEquals(error.<Integer>get("column").intValue(), 49);
        assertEquals(error.<String>get("message"), "\';\' expected");

    }

    String loadLogFromResources(String path) throws IOException{
        InputStream is = getClass().getResourceAsStream(path);
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer);
        return writer.toString();
    }

}
