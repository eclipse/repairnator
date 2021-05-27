package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.pipeline.github.GithubMainProcess;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPipelinebGithubMode {

    @Rule
    public TemporaryFolder workspaceFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testPipelineOnlyGitRepository() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                "--gitrepo",
                "--gitrepourl", "https://github.com/repairnator/failingProject",
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

    @Test
    public void testPipelineGitRepositoryAndBranch() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                "--gitrepo",
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--gitrepobranch", "astor-jkali-failure",
                "--repairTools", "AstorJKali",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        mainProc.run();
        assertEquals("TEST FAILURE", mainProc.getInspector().getFinding());
    }

    @Test
    public void testPipelineGitRepositoryAndCommitIdWithFailure() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                "--gitrepo",
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--gitrepobranch", "master",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        mainProc.run();
        assertEquals("TEST FAILURE", mainProc.getInspector().getFinding());
    }

    @Test
    public void testPipelineGitRepositoryAndCommitIdWithSuccess() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                "--gitrepo",
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--gitrepoidcommit", "7e1837df8db7a563fba65f75f7f477c43c9c75e9",
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

    @Test
    public void testSoraldWithSuccess() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                "--gitrepo",
                "--gitrepourl", "https://github.com/henry-lp/SonarQubeRepairTests",
                "--gitrepobranch", "master",
                "--sonarRules", "2116",
                "--repairTools", "Sorald",
                "--soraldMaxFixesPerRule", "1",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath(),
        });

        Patches patchNotifier = new Patches();
        mainProc.setPatchNotifier(patchNotifier);
        mainProc.run();
        assertEquals("PATCHED", mainProc.getInspector().getFinding());
        assertEquals(1, patchNotifier.allpatches.size());
    }

    @Test
    public void testPipelineGitRepositoryFirstCommit() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                "--gitrepo",
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--gitrepofirstcommit",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        mainProc.run();
        assertEquals("NOTBUILDABLE", mainProc.getInspector().getFinding());
    }

    class Patches implements PatchNotifier {
        List<RepairPatch> allpatches = new ArrayList<>();

        @Override
        public void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches) {
            allpatches.addAll(patches);
        }
    }
}
