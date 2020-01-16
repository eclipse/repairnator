package fr.inria.spirals.repairnator.notifier;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.PullRequest;
import fr.inria.jtravis.entities.Repository;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.JenkinsProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Skip Buggybuild check
 */
public class JenkinsPatchNotifierImpl implements PatchNotifier {
    public static final int LIMIT_NB_PATCH = 10;
    /* Move to Repairnator config later*/
    /*private static final boolean noTravisStep = true;*/

    private Logger logger = LoggerFactory.getLogger(JenkinsPatchNotifierImpl.class);
    private List<NotifierEngine> engines;

    public JenkinsPatchNotifierImpl(List<NotifierEngine> engines) {
        this.engines = engines;
    }

    @Override
    public void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches) {
        JobStatus jobStatus = inspector.getJobStatus();

        String subject = "[Repairnator] Patched build: " + ((JenkinsProjectInspector)inspector).getRepoSlug();
        String text = "Hurray !\n\n" +
                toolname + " has found " + patches.size() + " patch(es)"+".\n";

        String slug = ((JenkinsProjectInspector)inspector).getRepoSlug();
        String repoURL = ((JenkinsProjectInspector)inspector).getGitUrl();
        String branchName = ((JenkinsProjectInspector)inspector).getRemoteBranchName();

        String[] divideSlug = slug.split("/");
        String projectName = divideSlug[1];

        text += "Data about patches has been pushed on the following branch: "+ jobStatus.getGitBranchUrl()+".\n\n";
        text += "Follow those instruction to create a pull request:\n";
        text += "mkdir "+projectName+"\n";
        text += "cd "+projectName+"\n";
        text += "git init\n";

        text += "git fetch "+repoURL+" "+branchName+"\n";
        text += "git checkout -b patch FETCH_HEAD\n";
        text += "vi [file_to_patch]\n";
        text += "git commit -m \"tentative patch\" -a\n";

        if (jobStatus.isHasBeenForked()) {
            text += "git push "+jobStatus.getForkURL()+"\n";
        } else {
            text += "Then fork the repository ("+repoURL+") from Github interface\n";
            text += "git push [url to the fork]\n";
        }

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
