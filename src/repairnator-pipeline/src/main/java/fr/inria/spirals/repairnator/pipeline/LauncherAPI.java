package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;

/* Abstraction of a repairnator pipeline Launcher */
public interface LauncherAPI {

	/**
	  * start the Launcher
	  */
	void launch();

	/**
	  * the main process of the launcher after all preparation steps
	  * 
	  * @return true if the main process exited successfully, otherwise false
	  */
	boolean mainProcess();

	/**
	  * Get repairnator config to configure, usually at the preparation steps before mainProcess()
	  * 
	  * @return RepairntorConfig object
	  */
	RepairnatorConfig getConfig();

	/**
	  * JSAP args definitions for parsing String[] input args
	  * 
	  * @return JSAP to parse String[] args .
	  */
	JSAP defineArgs() throws JSAPException;

	/** 
	  * The project inspector for the project under repair. 
	  *
	  * @return ProjectInspector object
	  */
    ProjectInspector getInspector();

    /** 
	  * set a custom PatchNotifier to notify each time a patch is found.  
	  */
    void setPatchNotifier(PatchNotifier patchNotifier);
}