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

public class TestPipelinebSonarMode {

    @Test
    public void testSoraldWithSuccess() throws Exception {
        RepairnatorConfig.getInstance().setSonarRules(new String[]{"2116"});
        GithubMainProcess mainProc = (GithubMainProcess) MainProcessFactory.getGithubMainProcess(new String[]{
                    "--gitrepo",
                    "--gitrepourl", "https://github.com/henry-lp/SonarQubeRepairTests",
                    "--gitrepobranch", "master",
                    "--repairTools","Sorald",
                    "--workspace","./workspace-sonar-pipeline"
                });

        Patches patchNotifier = new Patches();
        mainProc.setPatchNotifier(patchNotifier);
        mainProc.run();
        assertEquals("PATCHED", mainProc.getInspector().getFinding());
        assertEquals(2, patchNotifier.allpatches.size());
    }


    class Patches implements PatchNotifier {
        List<RepairPatch> allpatches = new ArrayList<>();
        @Override
        public void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches) {
            allpatches.addAll(patches);
        }
    }
}
