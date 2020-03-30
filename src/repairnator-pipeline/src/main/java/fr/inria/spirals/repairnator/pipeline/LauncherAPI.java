package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;

public interface LauncherAPI {

	void launch();

	boolean mainProcess();

	RepairnatorConfig getConfig();

	JSAP defineArgs() throws JSAPException;

    ProjectInspector getInspector();

    void setPatchNotifier(PatchNotifier patchNotifier);
}