package fr.inria.spirals.repairnator.notifier;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

import java.util.List;

/**
 * Created by urli on 30/03/2017.
 */
public class FixerBuildNotifier extends AbstractNotifier {
    private boolean alreadyNotified;

    public FixerBuildNotifier(List<NotifierEngine> engines) {
        super(engines);
        this.alreadyNotified = false;
    }

    public void observe(ProjectInspector inspector) {
        JobStatus status = inspector.getJobStatus();
        if (!alreadyNotified && (status.getPipelineState() == PipelineState.FIXERBUILDCASE1 || status.getPipelineState() == PipelineState.FIXERBUILDCASE2)) {
            String slug = inspector.getRepoSlug();
            String subject = "Fixer build found: "+inspector.getPatchedBuild().getId()+" - "+slug;

            Build patchedBuild = inspector.getPatchedBuild();
            Build buggyBuild = inspector.getBuggyBuild();

            String text = "Hurray !\n\n" +
                    "A fixer build has been found for the following project: "+slug+".\n";

            if (status.isHasBeenPushed()) {
                text += "Data about this fixer build has been pushed on the following branch: "+status.getGitBranchUrl()+".\n";
            }

            text += "You can find several information on the following about it: \n";
            text += "\t Fixer build type: "+status.getPipelineState().name()+"\n" +
                    "\t Type of build: "+inspector.getBuildToBeInspected().getStatus().name()+"\n" +
                    "\t Date of the buggy build: "+Utils.formatCompleteDate(buggyBuild.getFinishedAt())+"\n" +
                    "\t Url of the buggy build: "+Utils.getTravisUrl(buggyBuild.getId(), slug)+"\n" +
                    "\t Date of the patched build: "+Utils.formatCompleteDate(patchedBuild.getFinishedAt())+"\n" +
                    "\t Url of the patched build: "+Utils.getTravisUrl(patchedBuild.getId(), slug)+"\n" +
                    "\t Contact: "+patchedBuild.getCommit().getCommitterEmail();

            this.notifyEngines(subject, text);
            this.alreadyNotified = true;
        }
    }
}
