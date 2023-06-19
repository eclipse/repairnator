package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
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

public class TestPipeline {

    @Rule
    public TemporaryFolder workspaceFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
    }


    @Test
    @Ignore //while fixing CI
    public void testPipeline() throws Exception {
        // requires env variable M2_HOME and GITHUB_OAUTH
        // (set in Travis config)
        // eg export M2_HOME=/usr/share/maven
        // from repairnator/failingBuild
        Launcher l = new Launcher(new String[]{
                "--jtravisendpoint", "https://api.travis-ci.com",
                "--build", "220925392",  //rerun onn 20-12-2022
                "--repairTools", "NPEFix",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });
        Patches patchNotifier = new Patches();
        l.setPatchNotifier(patchNotifier);
        l.mainProcess();
        assertEquals("PATCHED", l.getInspector().getFinding());
        assertEquals(10, patchNotifier.allpatches.size());
        assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));

    }

    @Test
    @Ignore //while fixing CI
    public void testPipelineOnlyGitRepository() throws Exception {
        Launcher l = new Launcher(new String[]{
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath(),
                "--launcherMode", "GIT_REPOSITORY"
        });

        Patches patchNotifier = new Patches();
        l.setPatchNotifier(patchNotifier);
        l.mainProcess();
        assertEquals("PATCHED", l.getInspector().getFinding());
        assertEquals(10, patchNotifier.allpatches.size());
        assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));
    }

    @Test
    @Ignore //while fixing CI
    public void testPipelineGitRepositoryAndBranch() throws Exception {
        Launcher l = new Launcher(new String[]{
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--gitrepobranch", "astor-jkali-failure",
                "--repairTools", "AstorJKali",
                "--launcherMode", "GIT_REPOSITORY",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        l.mainProcess();
        assertEquals("TEST FAILURE", l.getInspector().getFinding());
    }

    @Test
    @Ignore //while fixing CI
    public void testPipelineGitRepositoryAndCommitIdWithFailure() throws Exception {
        Launcher l = new Launcher(new String[]{
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--gitrepobranch", "no-infinite-loop",
                "--launcherMode", "GIT_REPOSITORY",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        l.mainProcess();
        assertEquals("TEST FAILURE", l.getInspector().getFinding());
    }

    @Test
    @Ignore //while fixing CI
    public void testPipelineGitRepositoryAndCommitIdWithSuccess() throws Exception {
        Launcher l = new Launcher(new String[]{
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--gitrepoidcommit", "7e1837df8db7a563fba65f75f7f477c43c9c75e9",
                "--launcherMode", "GIT_REPOSITORY",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        Patches patchNotifier = new Patches();
        l.setPatchNotifier(patchNotifier);
        l.mainProcess();
        assertEquals("PATCHED", l.getInspector().getFinding());
        assertEquals(10, patchNotifier.allpatches.size());
        assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));
    }

    @Ignore
    @Test
    public void testPipelineGitRepositoryFirstCommit() throws Exception {
        Launcher l = new Launcher(new String[]{
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--gitrepofirstcommit",
                "--launcherMode", "GIT_REPOSITORY",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        l.mainProcess();
        assertEquals("NOTBUILDABLE", l.getInspector().getFinding());
    }

    class Patches implements PatchNotifier {
        List<RepairPatch> allpatches = new ArrayList<>();

        @Override
        public void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches) {
            allpatches.addAll(patches);
        }
    }

    @Ignore
    @Test
    public void testPRLuc12() throws Exception {
        // reproducing the 12th PR of Luc
        // see https://github.com/eclipse/repairnator/issues/758
        Launcher l = new Launcher(new String[]{
                "--build", "395891390",
                "--repairTools", "NPEFix",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });
        Patches patchNotifier = new Patches();
        l.setPatchNotifier(patchNotifier);
        l.mainProcess();
        assertEquals("PATCHED", l.getInspector().getFinding());
        assertEquals(1, patchNotifier.allpatches.size());
        assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("hashtagStore != null"));
    }

}
