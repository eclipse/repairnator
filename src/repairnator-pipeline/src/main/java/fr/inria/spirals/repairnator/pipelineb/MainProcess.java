package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.notifier.PatchNotifier;

public interface MainProcess {
    IDefineJSAPArgs getIDefineJSAPArgs();
  
    IInitConfig getIInitConfig();

    IInitNotifiers getIInitNotifiers();

    IInitSerializerEngines getIInitSerializerEngines();

    void setPatchNotifier(PatchNotifier patchNotifier);

    PatchNotifier getPatchNotifier();

	boolean run();
}