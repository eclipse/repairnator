package fr.inria.spirals.repairnator.notifier;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;

public class GitRepositoryPatchNotifierImpl implements PatchNotifier {

	public static final int LIMIT_NB_PATCH = 10;

    private Logger logger = LoggerFactory.getLogger(GitRepositoryPatchNotifierImpl.class);
    private List<NotifierEngine> engines;

    public GitRepositoryPatchNotifierImpl(List<NotifierEngine> engines) {
        this.engines = engines;
    }

	@Override
    public void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches) {
        JobStatus jobStatus = inspector.getJobStatus();
        
        String subject = "[Repairnator] Patched repository: " + inspector.getProjectIdToBeInspected();
        String text = "Hurray !\n\n" +
                toolname + " has found " + patches.size() + " patch(es) for the following repository: " + inspector.getProjectIdToBeInspected() +".\n";

        text += "Data about patches has been pushed on the following branch: "+ jobStatus.getGitBranchUrl()+".\n\n";
        
        int limit;
        if (patches.size() > LIMIT_NB_PATCH) {
            text += "We show on the following the 10 first patches: \n\n";
            limit = LIMIT_NB_PATCH;
        } else {
            text += "We show on the following all computed patches: \n\n";
            limit = patches.size();
        }

        for (int i = 0; i < limit; i++) {
            text += "\t Patch n°" + i + ": \n";
            text += "\t" + patches.get(i).getDiff() + "\n\n";
        }

        for (NotifierEngine engine : this.engines) {
            engine.notify(subject, text);
        }
    }
}
