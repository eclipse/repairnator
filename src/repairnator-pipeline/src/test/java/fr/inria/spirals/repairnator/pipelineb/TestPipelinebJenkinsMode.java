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

public class TestPipelinebJenkinsMode {

    @Test
    public void testJenkinsOnlyGitRepositorys() throws Exception {
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getJenkinsPluginMainProcess(new String[]{
                    "--gitrepo",
                    "--gitrepourl", "https://github.com/surli/failingProject",
                    "--workspace","./workspace-jenkins-pipelinep"
                });

        Patches patchNotifier = new Patches();
        mainProc.setPatchNotifier(patchNotifier);
        mainProc.run();
        assertEquals("PATCHED", mainProc.getInspector().getFinding());
        assertEquals(10, patchNotifier.allpatches.size());
        assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));
    }

    class Patches implements PatchNotifier {
        List<RepairPatch> allpatches = new ArrayList<>();
        @Override
        public void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches) {
            allpatches.addAll(patches);
        }
    }
}
