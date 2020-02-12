package fr.inria.spirals.repairnator.process.maven;

import com.google.common.io.Files;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestMavenHelper {
	@Test
	public void testGetMavenModel() {
		File javaParserPom = new File("./src/test/resources/pom-examples/javaparser-pom.xml");
		File tempM2 = Files.createTempDir();

		Model model = MavenHelper.readPomXml(javaParserPom, tempM2.getAbsolutePath());
		assertNotNull(model);

		assertEquals(8, model.getModules().size());
	}
}