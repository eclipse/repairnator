package com.github.tdurieux.repair.maven.plugin;

import fr.inria.lille.repair.nopol.NopolStatus;
import org.apache.maven.plugin.Mojo;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

/*
@Ignore is ignored because this is ran with JUnit3. JUnit3 will run the test anyway.

We can't disable compatibility mode because of the following conflicts:

Method call super.tearDown() will not compile when class com.github.tdurieux.repair.maven.plugin.NopolMojoTest is converted to JUnit 4
Method call super.setUp() will not compile when class com.github.tdurieux.repair.maven.plugin.NopolMojoTest is converted to JUnit 4
Method call lookupConfiguredMojo(f, "nopol") will not compile when class com.github.tdurieux.repair.maven.plugin.NopolMojoTest is converted to JUnit 4

*/

public class NopolMojoTest /*extends BetterAbstractMojoTestCase*/ {
    /*
	private final String projectPath = "src/test/resources/projects/example1/";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Process mvn_clean_test = Runtime.getRuntime().exec("mvn clean test", null,	new File(projectPath));
		mvn_clean_test.waitFor();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Process mvn_clean = Runtime.getRuntime().exec("mvn clean", null,	new File(projectPath));
		mvn_clean.waitFor();
	}

    // This test is ignored since Repairnator has been moved to Java 11, as this test requires Java 8 or else Nopol fails.
	@Ignore
	@Test
	public void testNopolRepair() throws Exception {
		File f = getTestFile(projectPath + "pom.xml");
		Mojo mojo = lookupConfiguredMojo(f, "nopol");
		assertNotNull( mojo );
		assertTrue("Wrong class: "+mojo, mojo instanceof NopolMojo);

		NopolMojo repair = (NopolMojo) mojo;
		repair.execute();

		assertEquals(NopolStatus.PATCH, repair.getResult().getNopolStatus());
	}
	*/
}