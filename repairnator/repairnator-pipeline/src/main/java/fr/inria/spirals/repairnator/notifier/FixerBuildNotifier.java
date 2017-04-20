package fr.inria.spirals.repairnator.notifier;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.ProjectState;
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
        if (!alreadyNotified && (status.getState() == ProjectState.FIXERBUILDCASE1 || status.getState() == ProjectState.FIXERBUILDCASE2)) {
            String slug = inspector.getRepoSlug();
            String subject = "Fixer build found: "+inspector.getBuild().getId()+" - "+slug;

            Build lastBuild = inspector.getBuild();
            Build previousBuild = inspector.getPreviousBuild();

            String text = "Hurray !\n\n" +
                    "A fixer build has been found for the following project: "+slug+".\n";

            if (status.isHasBeenPushed()) {
                text += "Data about this fixer build has been pushed on the following branch: "+status.getGitBranchUrl()+".\n";
            }

            text += "You can find several information on the following about it: \n";
            text += "\t Fixer build type: "+status.getState().name()+"\n" +
                    "\t Type of build: "+inspector.getBuildToBeInspected().getStatus().name()+"\n" +
                    "\t Date of the first build: "+Utils.formatCompleteDate(previousBuild.getFinishedAt())+"\n" +
                    "\t Url of the first build: "+Utils.getTravisUrl(previousBuild.getId(), slug)+"\n" +
                    "\t Date of the second build: "+Utils.formatCompleteDate(lastBuild.getFinishedAt())+"\n" +
                    "\t Url of the second build: "+Utils.getTravisUrl(lastBuild.getId(), slug)+"\n" +
                    "\t Contact: "+lastBuild.getCommit().getCommitterEmail();

            this.notifyEngines(subject, text);
            this.alreadyNotified = true;
        }
    }
}
