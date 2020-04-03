package fr.inria.spirals.repairnator.pipeline;

import com.martiansoftware.jsap.FlaggedOption;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.martiansoftware.jsap.JSAP;

public class TestPipelineb {

    @Test
    public void testPipelineArgs() throws Exception {
        JSAP defaultJsap = (new DefaultDefineJSAPArgs()).defineArgs();

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
    public void testPipeline() throws Exception {
        // requires env variable M2_HOME and GITHUB_OAUTH
        // (set in Travis config)
        // eg export M2_HOME=/usr/share/maven
        // from surli/failingBuild
        DefaultMainProcess mainProc = (DefaultMainProcess) MainProcessFactory.getDefaultMainProcess(new String[]{"--build", "564711868",
            "--repairTools", "NPEFix",
            "--workspace","./workspace-pipelinep"});
		Patches patchNotifier = new Patches();
		mainProc.setPatchNotifier(patchNotifier);
		mainProc.run();
        assertEquals("PATCHED", mainProc.getInspector().getFinding());
		assertEquals(10, patchNotifier.allpatches.size());
		assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));

	}

    @Test
    public void testPipelineOnlyGitRepository() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                    "--gitrepo",
                    "--gitrepourl", "https://github.com/surli/failingProject",
                    "--workspace","./workspace-pipelinep"
                });

        Patches patchNotifier = new Patches();
		mainProc.setPatchNotifier(patchNotifier);
        mainProc.run();
        assertEquals("PATCHED", mainProc.getInspector().getFinding());
		assertEquals(10, patchNotifier.allpatches.size());
		assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));
	}

    @Test
    public void testPipelineGitRepositoryAndBranch() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                    "--gitrepo",
                    "--gitrepourl", "https://github.com/surli/failingProject",
                    "--gitrepobranch", "astor-jkali-failure",
                    "--repairTools", "AstorJKali",
                    "--workspace","./workspace-pipelinep"
                });

        mainProc.run();
        assertEquals("TEST FAILURE", mainProc.getInspector().getFinding());
	}

    @Test
    public void testPipelineGitRepositoryAndCommitIdWithFailure() throws Exception {        
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                    "--gitrepo",
                    "--gitrepourl", "https://github.com/surli/failingProject",
                    "--gitrepoidcommit", "f1233f8863d45141ae0ff17df897006f07e707bb",
                    "--workspace","./workspace-pipelinep"
                });

        mainProc.run();
        assertEquals("TEST FAILURE", mainProc.getInspector().getFinding());
	}

    @Test
    public void testPipelineGitRepositoryAndCommitIdWithSuccess() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                    "--gitrepo",
                    "--gitrepourl", "https://github.com/surli/failingProject",
                    "--gitrepoidcommit", "7e1837df8db7a563fba65f75f7f477c43c9c75e9",
                    "--workspace","./workspace-pipelinep"
                });

        Patches patchNotifier = new Patches();
		mainProc.setPatchNotifier(patchNotifier);
        mainProc.run();
        assertEquals("PATCHED", mainProc.getInspector().getFinding());
		assertEquals(10, patchNotifier.allpatches.size());
		assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));
	}
    
    @Test
    public void testJenkinsGitRepositoryAndBranchWithSucess() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getJenkinsPluginMainProcess(new String[]{
                    "--gitrepo",
                    "--gitrepourl", "https://github.com/surli/failingProject",
                    "--gitrepobranch", "astor-jkali-failure",
                    "--workspace","./workspace-pipelinep"
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
    public void testPipelineGitRepositoryFirstCommit() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                    "--gitrepo",
                    "--gitrepourl", "https://github.com/surli/failingProject",
                    "--workspace","./workspace-pipelinep",
                    "--gitrepofirstcommit"
                });

        mainProc.run();
        assertEquals("NOTBUILDABLE", mainProc.getInspector().getFinding());
	}

	@Ignore
	@Test
	public void testPRLuc12() throws Exception {
    	// reproducing the 12th PR of Luc
		// see https://github.com/eclipse/repairnator/issues/758

        DefaultMainProcess mainProc = (DefaultMainProcess) MainProcessFactory.getDefaultMainProcess(new String[]{"--build", "395891390", "--repairTools", "NPEFix", "--workspace","./workspace-pipelinep" });
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
