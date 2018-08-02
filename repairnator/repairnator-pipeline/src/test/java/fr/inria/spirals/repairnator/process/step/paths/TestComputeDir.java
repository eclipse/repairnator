package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.utils4tests.ProjectInspectorMocker;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TestComputeDir {
    @Test(expected = IllegalStateException.class)
    public void testComputeDirWithoutSettingDirType() {
        ComputeDir computeDir = new ComputeDir(ProjectInspectorMocker.mockProjectInspector(new JobStatus("/tmp")), true);
        computeDir.searchForDirs("/tmp", true);
    }

    private ComputeDir initComputeDir(String resourcePomDir, boolean isComputeSource, boolean allModules) {
        // the mock needs a JobStatus to work properly (see AbstractStep)
        JobStatus jobStatus = new JobStatus(resourcePomDir);

        // create a mock with the proper jobStatus
        ProjectInspector mockInspector = ProjectInspectorMocker.mockProjectInspector(jobStatus);

        // enrich the mock:
        // we need a path to the repository (here it points to the resource)
        // and a link to the local m2 repository (here we got a stub value)
        when(mockInspector.getRepoLocalPath()).thenReturn(resourcePomDir);
        when(mockInspector.getM2LocalPath()).thenReturn("/tmp");

        // we create the proper instance of the object to test
        ComputeDir computeDir = new ComputeDir(mockInspector, true);

        // init the object (else we get an exception)
        if (isComputeSource) {
            computeDir.setComputeDirType(ComputeDirType.COMPUTE_SOURCE_DIR);
        } else {
            computeDir.setComputeDirType(ComputeDirType.COMPUTE_TEST_DIR);
        }

        computeDir.setAllModules(allModules);

        return computeDir;
    }

    @Test
    public void testComputeSourceDirWithJrubyRootCall() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // it's a parent pom: true or false does not change anything

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/src/main/java"));
    }

    @Test
    public void testComputeSourceDirWithJrubyRootCallAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // it's a parent pom: true or false does not change anything

        assertNotNull(files);
        assertEquals(2, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/core/src/main/java"));
        assertTrue(files[1].getAbsolutePath().endsWith("src/test/resources/jruby/src/main/java"));
    }

    @Test
    public void testComputeSourceDirWithJrubyRootCallRecursive() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // it's a parent pom: true or false does not change anything

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/src/main/java"));
    }

    @Test
    public void testComputeSourceDirWithJrubyRootCallRecursiveAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // it's a parent pom: true or false does not change anything

        assertNotNull(files);
        assertEquals(2, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/core/src/main/java"));
        assertTrue(files[1].getAbsolutePath().endsWith("src/test/resources/jruby/src/main/java"));
    }

    @Test
    public void testComputeTestDirWithJrubyRoot() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // it's a parent pom: true or false does not change anything

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/lib/src/test/java"));
    }

    @Test
    public void testComputeTestDirWithJrubyRootAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // it's a parent pom: true or false does not change anything

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/lib/src/test/java"));
    }

    @Test
    public void testComputeTestDirWithJrubyRootCallRecursive() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // it's a parent pom: true or false does not change anything

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/lib/src/test/java"));
    }

    @Test
    public void testComputeTestDirWithJrubyRootCallRecursiveAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // it's a parent pom: true or false does not change anything

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/lib/src/test/java"));
    }

    @Test
    public void testComputeSourceDirWithJrubyLibCallRecursive() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/lib";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // only work in true mode: it must take the parent to get the children from the other module

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/src/main/java"));
    }

    @Test
    public void testComputeSourceDirWithJrubyLibCallRecursiveAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/lib";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // only work in true mode: it must take the parent to get the children from the other module

        assertNotNull(files);
        assertEquals(2, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/core/src/main/java"));
        assertTrue(files[1].getAbsolutePath().endsWith("src/test/resources/jruby/src/main/java"));
    }

    @Test
    public void testComputeSourceDirWithJrubyLibCall() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/lib";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // won't work here

        assertNull(files);
    }

    @Test
    public void testComputeSourceDirWithJrubyLibCallAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/lib";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // won't work here, even in all modules as it does not get the parent

        assertNull(files);
    }

    @Test
    public void testComputeTestDirWithJrubyLibCallRecursive() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/lib";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // works both ways here as the dir is in this module

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/lib/src/test/java"));
    }

    @Test
    public void testComputeTestDirWithJrubyLibCallRecursiveAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/lib";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // works both ways here as the dir is in this module

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/lib/src/test/java"));
    }

    @Test
    public void testComputeTestDirWithJrubyLibCall() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/lib";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // works both ways here as the dir is in this module

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/lib/src/test/java"));
    }

    @Test
    public void testComputeTestDirWithJrubyLibCallAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/lib";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // works both ways here as the dir is in this module

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/lib/src/test/java"));
    }

    @Test
    public void testComputeSourceDirWithJrubyCoreRecursive() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/core";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // works both ways here as the dir is in this module

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/core/src/main/java"));
    }

    @Test
    public void testComputeSourceDirWithJrubyCoreRecursiveAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/core";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // works both ways here as the dir is in this module

        assertNotNull(files);
        assertEquals(2, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/core/src/main/java"));
        assertTrue(files[1].getAbsolutePath().endsWith("src/test/resources/jruby/src/main/java"));
    }

    @Test
    public void testComputeSourceDirWithJrubyCore() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/core";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // works both ways here as the dir is in this module

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/core/src/main/java"));
    }

    @Test
    public void testComputeSourceDirWithJrubyCoreAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/core";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, true, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // works both ways here as the dir is in this module, but don't get the one from the parent

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/core/src/main/java"));
    }

    @Test
    public void testComputeTestDirWithJrubyCoreRecursive() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/core";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // works only when getting the parent here

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/lib/src/test/java"));
    }

    @Test
    public void testComputeTestDirWithJrubyCoreRecursiveAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/core";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, true); // works only when getting the parent here

        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue(files[0].getAbsolutePath().endsWith("src/test/resources/jruby/lib/src/test/java"));
    }

    @Test
    public void testComputeTestDirWithJrubyCore() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/core";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, false);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // won't work

        assertNull(files);
    }

    @Test
    public void testComputeTestDirWithJrubyCoreAllModules() {
        // root resource
        String resourcePomDir = "src/test/resources/jruby/core";

        ComputeDir computeDir = this.initComputeDir(resourcePomDir, false, true);

        // the method to test
        File[] files = computeDir.searchForDirs(resourcePomDir, false); // won't work

        assertNull(files);
    }
}
