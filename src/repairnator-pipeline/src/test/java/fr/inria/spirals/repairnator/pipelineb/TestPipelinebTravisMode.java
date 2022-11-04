package fr.inria.spirals.repairnator.pipeline;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.pipeline.travis.TravisDefineJSAPArgs;
import fr.inria.spirals.repairnator.pipeline.travis.TravisMainProcess;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TestPipelinebTravisMode {

    @Rule
    public TemporaryFolder workspaceFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testPipelineArgs() throws Exception {
        JSAP defaultJsap = (new TravisDefineJSAPArgs()).defineArgs();

        // the default repair tool
        assertEquals(1, ((FlaggedOption)defaultJsap.getByLongFlag("repairTools")).getDefault().length);
        assertEquals("NPEFix", ((FlaggedOption)defaultJsap.getByLongFlag("repairTools")).getDefault()[0]);

        // by default the activemq username and password should be blank
        assertEquals("", ((FlaggedOption)defaultJsap.getByLongFlag("activemqusername")).getDefault()[0]);
        assertEquals("", ((FlaggedOption)defaultJsap.getByLongFlag("activemqpassword")).getDefault()[0]);

        // non default value is accepted
        assertEquals("NopolAllTests", ((FlaggedOption)defaultJsap.getByLongFlag("repairTools")).getStringParser().parse("NopolAllTests"));

        // incorrect values are rejected
        try {
            ((FlaggedOption)defaultJsap.getByLongFlag("repairTools")).getStringParser().parse("garbage");
            fail();
        } catch (Exception expected) {}

    }

    @Test
    @Ignore //while fixing CI takes 50+ minutes pass on 03/11
    public void testPipeline() throws Exception {
        // requires env variable M2_HOME and GITHUB_OAUTH
        // (set in Travis config)
        // eg export M2_HOME=/usr/share/maven
        // from repairnator/failingBuild
        TravisMainProcess mainProc = (TravisMainProcess) MainProcessFactory.getTravisMainProcess(new String[]{
                "--jtravisendpoint", "https://api.travis-ci.com",
                "--build", "220925392",
                "--repairTools", "NPEFix",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });
		Patches patchNotifier = new Patches();
		mainProc.setPatchNotifier(patchNotifier);
		mainProc.run();
        assertEquals("PATCHED", mainProc.getInspector().getFinding());
		assertEquals(10, patchNotifier.allpatches.size());
		assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));

	}


	@Ignore
	@Test
	public void testPRLuc12() throws Exception {
    	// reproducing the 12th PR of Luc
		// see https://github.com/eclipse/repairnator/issues/758

        TravisMainProcess mainProc = (TravisMainProcess) MainProcessFactory.getTravisMainProcess(new String[]{
                "--build", "395891390",
                "--repairTools", "NPEFix",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });
		Patches patchNotifier = new Patches();
		mainProc.setPatchNotifier(patchNotifier);
		mainProc.run();
		assertEquals("PATCHED", mainProc.getInspector().getFinding());
		assertEquals(1, patchNotifier.allpatches.size());
		assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("hashtagStore != null"));
    }


    class Patches implements PatchNotifier {
        List<RepairPatch> allpatches = new ArrayList<>();
        @Override
        public void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches) {
            allpatches.addAll(patches);
        }
    }
}
