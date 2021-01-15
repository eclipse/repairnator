package com.github.tdurieux.repair.maven.plugin;

import fr.inria.spirals.npefix.resi.context.NPEOutput;
import org.apache.maven.plugin.Mojo;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class NPEfixMojoTest extends BetterAbstractMojoTestCase {
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
		Process mvn_clean = Runtime.getRuntime().exec("mvn clean",null, new File(projectPath));
		mvn_clean.waitFor();
	}

	public void testNPEFixRepair() throws Exception {
		File f = getTestFile(projectPath + "pom.xml");
		Mojo mojo = lookupConfiguredMojo(f, "npefix");
		assertNotNull( mojo );
		assertTrue("Wrong class: "+mojo, mojo instanceof NPEFixMojo);

		NPEFixMojo repair = (NPEFixMojo) mojo;
		repair.execute();

		List<File> patches = Arrays.asList(repair.getResultDirectory()
				.listFiles(((dir, name) -> name.startsWith("patches") && name.endsWith(".json"))));
		assertEquals(patches.size(), 1);
		JSONObject result = new JSONObject(patches.get(0));
		assertEquals(result.getJSONArray("executions").length(), 5);
		int successCount = 0;
		for (Object ob : result.getJSONArray("executions")) {
			if (((JSONObject) ob).getBoolean("success")) {
				successCount++;
			}
		}
		assertEquals(successCount, 3);
	}
}