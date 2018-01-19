package fr.inria.spirals.repairnator.notifier;

import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.nopol.PatchAndDiff;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;

import java.util.List;

/**
 * Created by urli on 30/03/2017.
 */
public class PatchNotifier extends AbstractNotifier {
    private boolean alreadyNotifiedForNopol;
    private boolean alreadyNotifiedForNPEFix;
    private boolean alreadyNotifiedForAstor;

    public PatchNotifier(List<NotifierEngine> engines) {
        super(engines);
    }

    public String notifyForNopol(JobStatus status) {
        if (alreadyNotifiedForNopol) {
            return "";
        }

        int i = 1;
        int totalFailure = status.getNopolInformations().size();
        String details = "Nopol Generated Patches: \n\n";

        for (NopolInformation nopolInformation : status.getNopolInformations()) {
            FailureLocation location = nopolInformation.getLocation();
            details += "\t Failure #"+i+" on "+totalFailure+"\n" +
                    "\t\t Concerned class: "+location.getClassName()+" ("+location.getNbFailures()+" failures / "+location.getNbErrors()+" errors)\n";

            for (int j = 0; j < Math.min(10, nopolInformation.getPatches().size()); j++) {
                PatchAndDiff patchAndDiff = nopolInformation.getPatches().get(j);
                Patch patch = patchAndDiff.getPatch();
                details += "\t\t Proposed patch #"+j+" in "+patch.getSourceLocation().toString()+" : "+patchAndDiff.getDiff()+"\n";
            }

            details += "\n\n";
            i++;
        }

        this.alreadyNotifiedForNopol = true;
        return details;
    }

    public String notifyForNPEFix(JobStatus status) {
        if (alreadyNotifiedForNPEFix) {
            return "";
        }

        int i = 1;
        int npePatches = status.getNpeFixPatches().size();

        String details = "NPEfix generated patches: \n\n";
        for (String diff : status.getNpeFixPatches()) {
            details += "\tPatch #"+i+" on "+npePatches+"\n";
            details += "\tDiff: "+diff+"\n\n";
            i++;
        }

        this.alreadyNotifiedForNPEFix = true;
        return details;
    }

    public String notifityForAstor(JobStatus status) {
        if (alreadyNotifiedForAstor) {
            return "";
        }

        int i = 1;
        int astorPatches = status.getAstorPatches().size();

        String details = "Astor generated patches: \n\n";
        for (String diff : status.getAstorPatches()) {
            details += "\tPatch #"+i+" on "+astorPatches+"\n";
            details += "\tDiff: "+diff+"\n\n";
            i++;
        }

        this.alreadyNotifiedForAstor = true;
        return details;
    }

    public void observe(ProjectInspector inspector) {
        JobStatus status = inspector.getJobStatus();

        if (status.isHasBeenPatched()) {
            String subject = "Patched build: "+inspector.getBuggyBuild().getId()+" - "+inspector.getBuggyBuild().getRepository().getSlug();

            String text = "Hurray !\n\n" +
                    "Patch(es) has been found for the following build: "+ Utils.getTravisUrl(inspector.getBuggyBuild().getId(), inspector.getBuggyBuild().getRepository().getSlug())+".\n";

            if (status.isHasBeenPushed()) {
                text += "Data about patches has been pushed on the following branch: "+status.getGitBranchUrl()+".\n";
            }

            text += "You may find several information on the following about those patches: \n";


            if (status.getNopolInformations() != null && !status.getNopolPatches().isEmpty() && !this.alreadyNotifiedForNopol) {
                text += this.notifyForNopol(status);

                this.notifyEngines("[NOPOL] "+subject, text);
            }

            if (status.getNpeFixPatches() != null && !status.getNpeFixPatches().isEmpty() && !this.alreadyNotifiedForNPEFix) {
                text += this.notifyForNPEFix(status);

                this.notifyEngines("[NPEFIX] "+subject, text);
            }

            if (status.getAstorPatches() != null && !status.getAstorPatches().isEmpty() && !this.alreadyNotifiedForAstor) {
                text += this.notifityForAstor(status);

                this.notifyEngines("[ASTOR] "+subject, text);
            }
        }
    }
}
