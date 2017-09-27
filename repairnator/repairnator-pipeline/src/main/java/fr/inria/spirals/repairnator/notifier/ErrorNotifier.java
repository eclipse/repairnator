package fr.inria.spirals.repairnator.notifier;

import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

import java.util.List;

/**
 * Created by urli on 07/09/2017.
 */
public class ErrorNotifier extends AbstractNotifier {

    private static ErrorNotifier instance;

    public static ErrorNotifier getInstance() {
        return instance;
    }

    public static ErrorNotifier getInstance(List<NotifierEngine> engines) {
        if (instance == null) {
            instance = new ErrorNotifier(engines);
        }
        return instance;
    }

    private ErrorNotifier(List<NotifierEngine> engines) {
        super(engines);
    }

    @Override
    public void observe(ProjectInspector inspector) {
        JobStatus status = inspector.getJobStatus();
        String subject = "URGENT! Error on buggy build "+inspector.getBuggyBuild().getId()+" on machine "+ Utils.getHostname();

        String message = "The following error has been encountered: ";
        if (status.getFatalError() != null) {
            message += "\n"+status.getFatalError().toString();
        }

        this.notifyEngines(subject, message);
    }
}
