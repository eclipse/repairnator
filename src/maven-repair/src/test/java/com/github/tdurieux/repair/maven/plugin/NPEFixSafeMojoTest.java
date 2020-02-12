package com.github.tdurieux.repair.maven.plugin;

import org.apache.maven.plugin.Mojo;

import java.io.File;

public class NPEFixSafeMojoTest extends BetterAbstractMojoTestCase {
    private final String projectPath = "src/test/resources/projects/example2/";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Process mvn_clean_test = Runtime.getRuntime().exec("mvn clean test", null,  new File(projectPath));
        mvn_clean_test.waitFor();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Process mvn_clean = Runtime.getRuntime().exec("mvn clean", null,    new File(projectPath));
        mvn_clean.waitFor();
    }

    public void testNPEFixRepair() throws Exception {
        File f = getTestFile(projectPath + "pom.xml");
        Mojo mojo = lookupConfiguredMojo(f, "npefix-safe");
        assertNotNull( mojo );
        assertTrue("Wrong class: "+mojo, mojo instanceof NPEFixSafeMojo);

        NPEFixSafeMojo repair = (NPEFixSafeMojo) mojo;
        repair.execute();

        assertTrue(repair.getResult().size() > 0);
    }
}