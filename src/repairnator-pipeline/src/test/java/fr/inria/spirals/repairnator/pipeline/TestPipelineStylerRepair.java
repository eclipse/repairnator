package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class TestPipelineStylerRepair {

	@Rule
	public TemporaryFolder workspaceFolder = new TemporaryFolder();

	@Rule
	public TemporaryFolder outputFolder = new TemporaryFolder();

	@After
	public void tearDown() throws IOException {
		RepairnatorConfig.deleteInstance();
	}

}
