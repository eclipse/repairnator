package fr.inria.spirals.repairnator.notifier;

import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;

import java.util.List;

/**
 * Created by urli on 30/03/2017.
 */
public class PatchNotifier extends AbstractNotifier {
    private boolean alreadyNotified;

    public PatchNotifier(List<NotifierEngine> engines) {
        super(engines);
        this.alreadyNotified = false;
    }

    public void observe(ProjectInspector inspector) {
        JobStatus status = inspector.getJobStatus();
        if (!alreadyNotified && status.isHasBeenPatched()) {
            String subject = "Patched build: "+inspector.getBuggyBuild().getId()+" - "+inspector.getBuggyBuild().getRepository().getSlug();



            String text = "Hurray !\n\n" +
                    "Patch(es) has been found for the following build: "+ Utils.getTravisUrl(inspector.getBuggyBuild().getId(), inspector.getBuggyBuild().getRepository().getSlug())+".\n";

            if (status.isHasBeenPushed()) {
                text += "Data about patches has been pushed on the following branch: "+status.getGitBranchUrl()+".\n";
            }

            text += "You may find several information on the following about those patches: \n";

            int totalPatches = 0;
            String details = "";
            if (status.getNopolInformations() != null && !status.getNopolInformations().isEmpty()) {
                int i = 1;
                int totalFailure = status.getNopolInformations().size();
                details += "Nopol Generated Patches: \n\n";
                for (NopolInformation nopolInformation : status.getNopolInformations()) {
                    totalPatches += nopolInformation.getPatches().size();

                    FailureLocation location = nopolInformation.getLocation();
                    details += "\t Failure #"+i+" on "+totalFailure+"\n" +
                            "\t\t Concerned class: "+location.getClassName()+" ("+location.getNbFailures()+" failures / "+location.getNbErrors()+" errors)\n";

                    for (int j = 0; j < Math.min(10, nopolInformation.getPatches().size()); j++) {
                        Patch patch = nopolInformation.getPatches().get(j);
                        details += "\t\t Proposed patch #"+j+" in "+patch.getSourceLocation().toString()+" : "+patch.asString()+"\n";
                        totalPatches++;
                    }

                    details += "\n\n";
                    i++;
                }
            }

            if (status.getNpeFixPatches() != null && !status.getNpeFixPatches().isEmpty()) {
                int i = 1;
                int npePatches = status.getNpeFixPatches().size();

                details += "NPEfix generated patches: \n\n";
                for (String diff : status.getNpeFixPatches()) {
                    details += "\tPatch #"+i+" on "+npePatches+"\n";
                    details += "\tDiff: "+diff+"\n\n";
                    totalPatches++;
                    i++;
                }
            }

            text += "\t Total patches found: "+totalPatches+".\n" + details;
            this.notifyEngines(subject, text);
            this.alreadyNotified = true;
        }
    }
}
