package fr.inria.spirals.repairnator.process.step.logParser;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TestLogParser {

    @Test
    public void TestLogParserCompilationError() throws IOException {

        LogParser p = new LogParser();
        String log = loadLogFromResources("/test-logparser/compilation-error.dat");

        p.parse(log);

        List<Element> errors = p.getErrors();

        assertEquals(2, errors.size());

        Element error = errors.get(0);

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
