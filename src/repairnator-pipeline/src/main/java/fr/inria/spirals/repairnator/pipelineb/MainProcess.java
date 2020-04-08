package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.notifier.PatchNotifier;

/* Main process of some procedure */
public interface MainProcess {

	/**
	  * Get the args definition behaviour before init the Main process creating.
	  *
	  *	@return JSAP args definitions
	  */
    IDefineJSAPArgs getIDefineJSAPArgs();
  	
  	/**
  	  * Get the Config initialization behaviour before init the Main process creating.
  	  *
  	  * @return Config initializtion object
  	  */
    IInitConfig getIInitConfig();

    /**
  	  * Get the Notifier initialization behaviour before init the Main process creating.
  	  *
  	  * @return Notifier initialization object
  	  */
    IInitNotifiers getIInitNotifiers();

    /**
  	  * Get the Serializer Engines initialization behaviour before init the Main process creating.
  	  *
  	  * @return serializer initialization object
  	  */
    IInitSerializerEngines getIInitSerializerEngines();


    void setPatchNotifier(PatchNotifier patchNotifier);

    PatchNotifier getPatchNotifier();

	boolean run();
}