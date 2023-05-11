package com.github.tdurieux.repair.maven.plugin;

import org.apache.maven.plugin.Mojo;

import java.io.File;

public class AstorMojoTest extends BetterAbstractMojoTestCase {

	private final String projectPath = "src/test/resources/projects/example3/";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Process mvn_clean_test = Runtime.getRuntime().exec("mvn clean test", null, new File(projectPath));
		mvn_clean_test.waitFor();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Process mvn_clean = Runtime.getRuntime().exec("mvn clean", null,	new File(projectPath));
		mvn_clean.waitFor();
	}

	public void testGenProgRepair() throws Exception {
		File f = getTestFile(projectPath + "pom.xml");
		Mojo mojo = lookupConfiguredMojo(f, "jGenProg");
		assertNotNull( mojo );
		assertTrue("Wrong class: "+mojo, mojo instanceof GenProgMojo);

		GenProgMojo repair = (GenProgMojo) mojo;
		repair.execute();
	}

	public void testKaliRepair() throws Exception {
		File f = getTestFile(projectPath + "pom.xml");
		Mojo mojo = lookupConfiguredMojo(f, "jKali");
		assertNotNull( mojo );
		assertTrue("Wrong class: "+mojo, mojo instanceof KaliMojo);

		KaliMojo repair = (KaliMojo) mojo;
		repair.execute();
	}

	public void testCardumenRepair() throws Exception {
		File f = getTestFile(projectPath + "pom.xml");
		Mojo mojo = lookupConfiguredMojo(f, "cardumen");
		assertNotNull( mojo );
		assertTrue("Wrong class: "+mojo, mojo instanceof CardumenMojo);

		CardumenMojo repair = (CardumenMojo) mojo;
		repair.execute();
	}
}