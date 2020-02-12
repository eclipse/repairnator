package fr.inria.spirals.repairnator.notifier;

import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

import java.util.List;

/**
 * Created by urli on 30/03/2017.
 */
public abstract class AbstractNotifier {

    private List<NotifierEngine> engines;

    public AbstractNotifier(List<NotifierEngine> engines) {
        this.engines = engines;
    }

    protected void notifyEngines(String subject, String text) {
        for (NotifierEngine engine : this.engines) {
            engine.notify(subject, text);
        }
    }

    public abstract void observe(ProjectInspector inspector);
}
