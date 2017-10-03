package fr.inria.spirals.repairnator.process.maven.output;

import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 03/10/2017.
 */
public class TestMavenFilterTestOutputHandler {

    @Test
    public void testFilterLogWillWork() throws IOException {
        String resourcePath = "./src/test/resources/log-test-failures.txt";
        List<String> lines = Files.readAllLines(new File(resourcePath).toPath());

        ProjectInspector inspector = mock(ProjectInspector.class);
        Path tmp = Files.createTempDirectory("test-filter");
        when(inspector.getRepoLocalPath()).thenReturn(tmp.toAbsolutePath().toString());

        JobStatus jobStatus = new JobStatus(tmp.toAbsolutePath().toString());
        when(inspector.getJobStatus()).thenReturn(jobStatus);
        MavenFilterTestOutputHandler filterTest = new MavenFilterTestOutputHandler(inspector, "test");

        for (String s : lines) {
            filterTest.consumeLine(s);
        }

        assertTrue(filterTest.isFailingWithTest());
        assertEquals(40, filterTest.getRunningTests());
        assertEquals(0, filterTest.getFailingTests());
        assertEquals(9, filterTest.getErroringTests());
        assertEquals(3, filterTest.getSkippingTests());
    }
}
