package fr.inria.spirals.repairnator.process.maven.output;

import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.utils4tests.ProjectInspectorMocker;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 03/10/2017.
 */
public class TestMavenFilterTestOutputHandler {

    private File tmpDir;

    @After
    public void tearDown() throws IOException {
        GitHelper.deleteFile(tmpDir);
    }

    @Test
    public void testFilterLogWillWork() throws IOException {
        String resourcePath = "./src/test/resources/build-logs/log-test-failures.txt";
        List<String> lines = Files.readAllLines(new File(resourcePath).toPath());

        tmpDir = Files.createTempDirectory("test-filter").toFile();

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir.getAbsolutePath());

        MavenHelper mavenHelper = mock(MavenHelper.class);
        when(mavenHelper.getInspector()).thenReturn(inspector);
        when(mavenHelper.getName()).thenReturn("test");
        MavenFilterTestOutputHandler filterTest = new MavenFilterTestOutputHandler(mavenHelper);

        for (String s : lines) {
            filterTest.consumeLine(s);
        }

        assertTrue(filterTest.isFailingWithTest());
        assertEquals(40, filterTest.getRunningTests());
        assertEquals(0, filterTest.getFailingTests());
        assertEquals(9, filterTest.getErroringTests());
        assertEquals(3, filterTest.getSkippingTests());
    }

    @Test
    public void testFilterLogWillWork2() throws IOException {
        String resourcePath = "./src/test/resources/build-logs/log-test-druidio.txt";
        List<String> lines = Files.readAllLines(new File(resourcePath).toPath());

        tmpDir = Files.createTempDirectory("test-filter").toFile();

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir.getAbsolutePath());

        MavenHelper mavenHelper = mock(MavenHelper.class);
        when(mavenHelper.getInspector()).thenReturn(inspector);
        when(mavenHelper.getName()).thenReturn("test");
        MavenFilterTestOutputHandler filterTest = new MavenFilterTestOutputHandler(mavenHelper);

        for (String s : lines) {
            filterTest.consumeLine(s);
        }

        assertFalse(filterTest.isFailingWithTest());
        assertEquals(77351*2, filterTest.getRunningTests());
        assertEquals(0, filterTest.getFailingTests());
        assertEquals(0, filterTest.getErroringTests());
        assertEquals((52+6+1)*2, filterTest.getSkippingTests());
    }
}
