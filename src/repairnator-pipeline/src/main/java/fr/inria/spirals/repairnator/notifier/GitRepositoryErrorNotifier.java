package fr.inria.spirals.repairnator.notifier;

import java.util.List;

import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.utils.Utils;

public class GitRepositoryErrorNotifier extends AbstractNotifier {

    private static GitRepositoryErrorNotifier instance;

    public static GitRepositoryErrorNotifier getInstance() {
        return instance;
    }

    public static GitRepositoryErrorNotifier getInstance(List<NotifierEngine> engines) {
        if (instance == null) {
            instance = new GitRepositoryErrorNotifier(engines);
        }
        return instance;
    }

    private GitRepositoryErrorNotifier(List<NotifierEngine> engines) {
        super(engines);
    }

    @Override
    public void observe(ProjectInspector inspector) {
        JobStatus status = inspector.getJobStatus();
        String subject = "";
        
        subject = "URGENT! Error on repository " + inspector.getProjectIdToBeInspected() + " on machine "+ Utils.getHostname();
        
        String message = "The following error has been encountered: ";
        if (status.getFatalError() != null) {
            message += "\n"+status.getFatalError().toString();
        }

        this.notifyEngines(subject, message);
    }

}
