package fr.inria.spirals.repairnator.notifier;

import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;

import java.util.Date;
import java.util.List;

/**
 * Created by urli on 09/05/2017.
 */
public class EndProcessNotifier {
    private List<NotifierEngine> engines;
    private Date launchDate;
    private String processName;

    public EndProcessNotifier(List<NotifierEngine> engines, String processName) {
        this.launchDate = new Date();
        this.processName = processName;
        this.engines = engines;
    }

    public void notifyEnd() {
        String subject = "Process "+processName+" on "+Utils.getHostname()+" finished";
        String message = "The following process: "+processName+" launched on "+ Utils.getHostname()+" the "+this.launchDate.toString()+" has just finished the "+new Date().toString()+".";

        for (NotifierEngine engine : this.engines) {
            engine.notify(subject, message);
        }
    }
}
