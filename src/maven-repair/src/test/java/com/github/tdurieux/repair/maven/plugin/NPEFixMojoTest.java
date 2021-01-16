package com.github.tdurieux.repair.maven.plugin;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.Mojo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class NPEFixMojoTest extends BetterAbstractMojoTestCase {
    private final String projectPath = "src/test/resources/projects/example2/";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ProcessBuilder pb = new ProcessBuilder("mvn", "clean");
        pb.directory(new File(projectPath));
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Process mvn_clean = Runtime.getRuntime().exec("mvn clean", null, new File(projectPath));
        mvn_clean.waitFor();
    }

    public void testNPEFixRepair() throws Exception {
        File f = getTestFile(projectPath + "pom.xml");
        Mojo mojo = lookupConfiguredMojo(f, "npefix");
        assertNotNull(mojo);
        assertTrue("Wrong class: " + mojo, mojo instanceof NPEFixMojo);

        NPEFixMojo repair = (NPEFixMojo) mojo;
        repair.execute();

        List<File> patches = Arrays.asList(repair.getResultDirectory()
                .listFiles(((dir, name) -> name.startsWith("patches") && name.endsWith(".json"))));
        assertEquals(patches.size(), 1);

        InputStream is = new BufferedInputStream(new FileInputStream(patches.get(0)));
        JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse(IOUtils.toString(is));

        assertEquals(((JSONArray) result.get("executions")).size(), 5);
        int successCount = 0;
        for (Object ob : (JSONArray) result.get("executions")) {
            JSONObject res = (JSONObject) ((JSONObject) ob).get("result");
            if (res.get("success").equals(true)) {
                successCount++;
            }
        }
        assertEquals(successCount, 3);
    }
}